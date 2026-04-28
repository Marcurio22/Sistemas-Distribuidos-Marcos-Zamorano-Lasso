package com.marcoszamorano.practica2.dto.lab;

import java.util.ArrayList;
import java.util.List;

public class PokemonDetailsView {

    private Integer id;
    private String name;
    private Integer height;
    private Integer weight;
    private Integer baseExperience;
    private String officialArtwork;
    private String frontDefault;
    private List<String> types = new ArrayList<>();
    private List<String> abilities = new ArrayList<>();
    private List<PokemonStatView> stats = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        if (name == null || name.isBlank()) {
            return "Pokémon";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHeight() {
        return height;
    }

    public String getHeightDisplay() {
        return height == null ? "-" : (height / 10.0) + " m";
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getWeightDisplay() {
        return weight == null ? "-" : (weight / 10.0) + " kg";
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getBaseExperience() {
        return baseExperience;
    }

    public void setBaseExperience(Integer baseExperience) {
        this.baseExperience = baseExperience;
    }

    public String getOfficialArtwork() {
        return officialArtwork;
    }

    public void setOfficialArtwork(String officialArtwork) {
        this.officialArtwork = officialArtwork;
    }

    public String getFrontDefault() {
        return frontDefault;
    }

    public void setFrontDefault(String frontDefault) {
        this.frontDefault = frontDefault;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }

    public List<PokemonStatView> getStats() {
        return stats;
    }

    public void setStats(List<PokemonStatView> stats) {
        this.stats = stats;
    }
}