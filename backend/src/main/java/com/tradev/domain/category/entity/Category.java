package com.tradev.domain.category.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private int sortOrder;

    public Category(Category parent, String name, int depth, int sortOrder) {
        this.parent = parent;
        this.name = name;
        this.depth = depth;
        this.sortOrder = sortOrder;
    }
}
