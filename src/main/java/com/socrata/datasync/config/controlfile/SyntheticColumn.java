package com.socrata.datasync.config.controlfile;

import java.util.HashMap;
import java.util.Map;

public abstract class SyntheticColumn {
    public abstract Map<String, String> findComponentColumns();

    @Override
    public String toString() {
        return findComponentColumns().toString();
    }
}
