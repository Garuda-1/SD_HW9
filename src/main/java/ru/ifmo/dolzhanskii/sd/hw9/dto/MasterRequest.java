package ru.ifmo.dolzhanskii.sd.hw9.dto;

import java.io.OutputStream;
import java.util.List;

import lombok.Value;
import ru.ifmo.dolzhanskii.sd.hw9.client.SearchClient;

@Value
public class MasterRequest {

    String query;
    List<SearchClient> searchProcessors;
    OutputStream outputStream;
}
