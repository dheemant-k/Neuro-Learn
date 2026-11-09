package com.neurolearn.dto;

import java.util.List;

/** eliminatedOptions is filled for FIFTY_FIFTY, empty for CLUE. hintsUsed counts this question's hints so far. */
public record HintResponse(String type, List<String> eliminatedOptions, int hintsUsed) {
}
