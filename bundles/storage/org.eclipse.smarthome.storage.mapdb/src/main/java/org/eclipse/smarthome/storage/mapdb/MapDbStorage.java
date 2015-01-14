/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.storage.mapdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.smarthome.core.storage.Storage;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The MapDbStorage is concrete implementation of the {@link Storage} interface.
 * It stores the key-value pairs in files. This Storage serializes and deserializes
 * the given values using their JSON representation (generated by {@code Gson}.
 * This transformation should help maintaining version compatibility of the stored
 * data. 
 * 
 * @author Thomas.Eichstaedt-Engelen - Initial Contribution and API
 * @author Alex Tugarev - Loading with Class.forName() if classLoader is null
 */
public class MapDbStorage<T> implements Storage<T> {

	private static final String TYPE_SEPARATOR = "@@@";

	private final Logger logger = 
		LoggerFactory.getLogger(MapDbStorage.class);
	
	private DB db;
	private ClassLoader classLoader;
	private Map<String, String> map;
	
	private transient Gson mapper;
	
	
	public MapDbStorage(DB db, String name, ClassLoader classLoader) {
		this.db = db;
		this.classLoader = classLoader;
		this.map = db.createTreeMap(name).makeOrGet();
		this.mapper = new GsonBuilder().registerTypeAdapterFactory(new PropertiesTypeAdapterFactory()).create();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T put(String key, T value) {
		String previousValue = map.put(key, serialize(value));
		db.commit();
		return deserialize(previousValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T remove(String key) {
		String removedElement = map.remove(key);
		db.commit();
		return deserialize(removedElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T get(String key) {
		return deserialize(map.get(key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getKeys() {
		return map.keySet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<T> getValues() {
		Collection<T> values = new ArrayList<T>();
		for (String key : getKeys()) {
			values.add(get(key));
		}
		return values;
	}
	
	/**
	 * Transforms the given {@code value} into its JSON representation using
	 * {@code Gson}. Since we do not know the type of {@code value} while
	 * deserializing it afterwards we prepend its qualified type name to the
	 * JSON String.
	 * 
	 * @param value the {@code value} to store
	 * @return the JSON document prepended with the qualified type name of {@code value}
	 */
	private String serialize(T value) {
		
		if (value == null) {
			throw new IllegalArgumentException("Cannot serialize NULL");
		}
		
		String valueTypeName = value.getClass().getName();
		String valueAsString = mapper.toJson(value);
		String concatValue = valueTypeName + TYPE_SEPARATOR + valueAsString;
		
		logger.trace("serialized value '{}' to MapDB", concatValue);
		return concatValue;
	}
	
	/**
	 * Deserializes and instantiates an object of type {@code T} out of the
	 * given JSON String. A special classloader (other than the one of the
	 * MapDB bundle) is used in order to load the classes in the context of
	 * the calling bundle.
	 * 
	 * @param json
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T deserialize(String json) {
		
		if (json == null) {
			// nothing to deserialize
			return null;
		}

		String[] concatValue = json.split(TYPE_SEPARATOR);
		String valueTypeName = concatValue[0];
		String valueAsString = concatValue[1];
		
		T value = null;
		try {
			// load required class within the given bundle context
            Class<T> loadedValueType = null;
            if (classLoader == null) {
                loadedValueType = (Class<T>) Class.forName(valueTypeName);
            } else {
                loadedValueType = (Class<T>) classLoader.loadClass(valueTypeName);
            }
			
			value = mapper.fromJson(valueAsString, loadedValueType);
			logger.trace("deserialized value '{}' from MapDB", value);
		} catch (Exception e) {
			logger.warn("Couldn't deserialize value '{}'. Root cause is: {}", json, e.getMessage());
		}
		
		return value;
	}
	
}
