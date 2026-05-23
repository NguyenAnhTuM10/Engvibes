package com.englishapp.speak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Internal DTO for parsing GPT Audio JSON response (IELTS 0-9 schema). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IeltsFeedback(
        @JsonProperty("transcript")    String transcript,
        @JsonProperty("fluency")       double fluency,
        @JsonProperty("grammar")       double grammar,
        @JsonProperty("vocabulary")    double vocabulary,
        @JsonProperty("pronunciation") double pronunciation,
        @JsonProperty("overall")       double overall,
        @JsonProperty("feedback")      String feedback
) {}
