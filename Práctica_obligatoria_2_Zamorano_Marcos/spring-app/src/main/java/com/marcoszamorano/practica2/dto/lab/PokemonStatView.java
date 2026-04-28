package com.marcoszamorano.practica2.dto.lab;

public class PokemonStatView {

    private String name;
    private int value;

    public PokemonStatView() {
    }

    public PokemonStatView(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getPercent() {
        return Math.min(value, 150) * 100 / 150;
    }

    public String getDisplayName() {
        return switch (name) {
            case "hp" -> "HP";
            case "attack" -> "Ataque";
            case "defense" -> "Defensa";
            case "special-attack" -> "Ataque especial";
            case "special-defense" -> "Defensa especial";
            case "speed" -> "Velocidad";
            default -> name;
        };
    }
}