package com.minecave.ranks.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Table {
	private String name;
	private String statNames = "";
	private String namesWithTypes = "", delimitedValues = "";
	private Map<String, String> stats;
	private static List<Table> tables = Lists.newArrayList();
	
	// UUID has to be the first stat in every list except ones with playerSpecific set to false.
	public static final Table RANKS;
	public static final Table UUIDS;
	
	// RANKS
	static {
		Map<String, String> map = Maps.newHashMap();
		map.put("uuid varchar(200) UNIQUE", "<uuid>");
		map.put("groups varchar(200)", "''");
		RANKS = new Table("RANKS", map, "uuid varchar(200) UNIQUE");
	}
	
	// UUIDS
	static {
		Map<String, String> map = Maps.newHashMap();
		map.put("uuid varchar(200) UNIQUE", "<name>");
		map.put("actual_uuid varchar(200)", "<uuid>");
		UUIDS = new Table("UUIDS", map, "uuid varchar(200) UNIQUE");
	}
	
	public Table(String name, Map<String, String> stats, String firstKey) {
		this.name = name;
		this.stats = stats;
		
		List<String> keys = new ArrayList<String>(stats.keySet());
		List<String> values = new ArrayList<String>(stats.values());
		
		if (!keys.get(0).equals(firstKey)) {
			Collections.reverse(keys);
			Collections.reverse(values);
		}
		
		for (String nameAndType : keys) {
			if (!namesWithTypes.equals(""))
				namesWithTypes += ", ";
			namesWithTypes += nameAndType;
			
			if (!statNames.equals(""))
				statNames += ", ";
			statNames += nameAndType.split(" ")[0];
		}
		for (String value : values) {
			if (!delimitedValues.equals(""))
				delimitedValues += ", ";
			delimitedValues += value;
		}
		
		tables.add(this);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String name() {
		return name;
	}
	
	public Map<String, String> getStats() {
		return stats;
	}
	
	public String getStatNames() {
		return statNames;
	}
	
	public String getNamesWithTypes() {
		return namesWithTypes;
	}
	
	public String getDelimitedValues() {
		return delimitedValues;
	}
	
	public String getDelimitedValues(String uuid) {
		return StringUtils.replace(delimitedValues, "<uuid>", "'" + uuid + "'");
	}
	
	public String getDelimitedValues(String name, String uuid) {
		String values = delimitedValues;
		values = StringUtils.replace(values, "<name>", "'" + name + "'");
		values = StringUtils.replace(values, "<uuid>", "'" + uuid + "'");
		return values;
	}
	
	public static Table getTable(String name) {
		for (Table table : tables) {
			if (table.name().equals(name))
				return table;
		}
		return null;
	}
}