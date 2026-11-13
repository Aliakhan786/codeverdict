package com.aaliya.codeverdict.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Badge catalogue row (seeded by V2__achievements_seed.sql). */
@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 40)
    private String icon;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public int getXpReward() { return xpReward; }
    public int getSortOrder() { return sortOrder; }
}
