package ru.ifmo.dolzhanskii.sd.hw9.dto;

import java.util.List;

import lombok.Value;

@Value
public class SearchResponse {

    boolean ok;
    String name;
    List<String> results;
}
