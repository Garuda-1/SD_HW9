package ru.ifmo.dolzhanskii.sd.hw9.client;

import java.io.IOException;
import java.util.List;

public interface SearchClient {

    List<String> searchTopResponses(String query) throws IOException;

    String getName();
}
