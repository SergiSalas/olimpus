package com.sergisalas.olimpus.safety.domain;

import java.util.List;
import java.util.UUID;

public interface BlockRepository {

    void save(Block block);

    List<Block> byBlocker(UUID blocker);
}
