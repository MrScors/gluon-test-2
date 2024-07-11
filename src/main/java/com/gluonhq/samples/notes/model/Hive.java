package com.gluonhq.samples.notes.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Hive {
    private int id;
    private List<Integer> occupiedBoxes;
    private long creationDate;
    private boolean alive;

    public Hive() {
        this.creationDate = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        this.alive = true;
    }

}