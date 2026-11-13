package com.aaliya.codeverdict.proof;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads the XML report written by the JUnit console launcher (--reports-dir). */
public final class JUnitReportParser {

    /** Result of one test class. */
    public record ClassResult(int tests, int failed, boolean timedOut, String firstFailure, Integer solutionLine) {
        public boolean passed() {
            return tests > 0 && failed == 0;
        }
    }

    private static final Pattern SOLUTION_LINE = Pattern.compile("\\(Solution\\.java:(\\d+)\\)");

    /** @return test class name -> result. Empty map if no report exists (e.g. the JVM was killed). */
    public Map<String, ClassResult> parse(Path reportsDir) {
        Map<String, ClassResult> results = new HashMap<>();
        if (!Files.isDirectory(reportsDir)) {
            return results;
        }
        try (Stream<Path> files = Files.list(reportsDir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".xml")).toList()) {
                parseFile(f, results);
            }
        } catch (IOException e) {
            return results;
        }
        return results;
    }

    private void parseFile(Path file, Map<String, ClassResult> results) {
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            doc = b.parse(file.toFile());
        } catch (Exception e) {
            return;
        }
        NodeList cases = doc.getElementsByTagName("testcase");
        Map<String, int[]> counts = new HashMap<>();
        Map<String, String> firstFailure = new HashMap<>();
        Map<String, Integer> line = new HashMap<>();
        Map<String, Boolean> timeout = new HashMap<>();
        for (int i = 0; i < cases.getLength(); i++) {
            Element tc = (Element) cases.item(i);
            String cls = tc.getAttribute("classname");
            int[] c = counts.computeIfAbsent(cls, k -> new int[2]);
            c[0]++;
            Element problem = firstChild(tc, "failure");
            if (problem == null) {
                problem = firstChild(tc, "error");
            }
            if (problem != null) {
                c[1]++;
                String type = problem.getAttribute("type");
                String msg = problem.getAttribute("message");
                String body = problem.getTextContent() == null ? "" : problem.getTextContent();
                boolean isTimeout = type.endsWith("TimeoutException") || msg.contains("timed out after");
                if (isTimeout) {
                    timeout.put(cls, true);
                }
                if (!firstFailure.containsKey(cls)) {
                    String simpleType = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;
                    String text = isTimeout ? "Time limit exceeded (possible infinite loop)"
                            : simpleType + (msg.isBlank() ? "" : ": " + msg);
                    firstFailure.put(cls, abbreviate(text, 600));
                    Matcher m = SOLUTION_LINE.matcher(body);
                    if (m.find()) {
                        line.put(cls, Integer.parseInt(m.group(1)));
                    }
                }
            }
        }
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            String cls = e.getKey();
            results.put(cls, new ClassResult(e.getValue()[0], e.getValue()[1],
                    timeout.getOrDefault(cls, false), firstFailure.get(cls), line.get(cls)));
        }
    }

    private static Element firstChild(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element el && el.getTagName().equals(tag)) {
                return el;
            }
        }
        return null;
    }

    private static String abbreviate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
