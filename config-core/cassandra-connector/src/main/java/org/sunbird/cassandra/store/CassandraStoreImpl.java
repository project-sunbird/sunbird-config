package org.sunbird.cassandra.store;

import com.datastax.driver.core.Row;

import java.util.List;
import java.util.Map;

public class CassandraStoreImpl extends CassandraStore {

    @Override
    public void initialise(String keyspace, String table, String objectType, boolean index) {
        super.initialise(keyspace, table, objectType, index);
    }

    @Override
    public void insert(Object idValue, Map<String, Object> request) {
        super.insert(idValue, request);

    }

    @Override
    public void upsertRecord(Map<String, Object> request) {
        super.upsertRecord(request);
    }

    @Override
    public List<Row> read(String key, Object value) {
        List<Row> records = super.read(key, value);
        return records;
    }

    @Override
    public Row getLatestRecordTimestamp(String timestampColumn, String partitionColKey, String partitionColVal) {
        Row record = super.getLatestRecordTimestamp(timestampColumn, partitionColKey, partitionColVal);
        return record;
    }

    @Override
    public Row getLatestRecord(String timestampColumn, Long lastRefreshTimestamp) {
        Row record = super.getLatestRecord(timestampColumn, lastRefreshTimestamp);
        return record;
    }

    @Override
    public Row getRandomOneRecord() {
        Row record = super.getRandomOneRecord();
        return record;
    }
}
