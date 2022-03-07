package ru.ifmo.dolzhanskii.sd.hw9.dto;

import lombok.Value;
import ru.ifmo.dolzhanskii.sd.hw9.client.SearchClient;

@Value
public class SearchRequest {

    String query;
    SearchClient searchClient;
}
