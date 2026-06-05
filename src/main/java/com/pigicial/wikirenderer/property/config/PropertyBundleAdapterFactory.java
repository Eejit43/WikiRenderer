package com.pigicial.wikirenderer.property.config;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.DoubleProperty;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.property.SerializablePropertyBundle;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class PropertyBundleAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!SerializablePropertyBundle.class.isAssignableFrom(type.getRawType())){
            return null;
        }

        return new PropertyBundleAdapter<>(gson, type);
    }

    private static class PropertyBundleAdapter<T> extends TypeAdapter<T> {
        private final Gson gson;
        private final TypeToken<T> type;

        public PropertyBundleAdapter(Gson gson, TypeToken<T> type) {
            this.gson = gson;
            this.type = type;
        }

        @Override
        public void write(JsonWriter out, T bundle) throws IOException {
            out.beginObject();
            for (Field field : getAllFields(bundle.getClass())) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);

                try {
                    Object object = field.get(bundle);
                    if (object instanceof Property<?> prop) {
                        object = prop.get();
                    }

                    out.name(field.getName());
                    if (object == null) {
                        out.nullValue();
                    } else {
                        TypeAdapter adapter = gson.getAdapter(object.getClass());
                        adapter.write(out, object);
                    }
                } catch (Exception e) {
                    WikiRenderer.LOGGER.error("Failed to write config field: {}", field.getName(), e);
                }
            }
            out.endObject();
        }

        @Override
        public T read(JsonReader in) throws IOException {
            try {
                T bundle = (T) type.getRawType().getConstructor().newInstance();
                in.beginObject();

                // Map fields by name for quick lookup during reading
                Map<String, Field> fieldMap = new HashMap<>();
                for (Field f : getAllFields(bundle.getClass())) {
                    fieldMap.put(f.getName(), f);
                }

                while (in.hasNext()) {
                    String name = in.nextName();
                    Field field = fieldMap.get(name);
                    if (field == null) {
                        WikiRenderer.LOGGER.warn("Couldn't find config field {}, ignoring", name);
                        in.skipValue();
                        continue;
                    }

                    if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                        in.skipValue();
                        continue;
                    }

                    field.setAccessible(true);
                    Object fieldObject = field.get(bundle);

                    try {
                        switch (fieldObject) {
                            case IntProperty intProperty -> {
                                try {
                                    intProperty.set(in.nextInt());
                                } catch (NumberFormatException e) {
                                    in.skipValue();
                                    WikiRenderer.LOGGER.warn("Couldn't parse integer for field {} as it's not a valid number, ignoring", field.getName());
                                }
                            }
                            case DoubleProperty doubleProperty -> {
                                try {
                                    doubleProperty.set(in.nextDouble());
                                } catch (NumberFormatException e) {
                                    in.skipValue();
                                    WikiRenderer.LOGGER.warn("Couldn't parse double for field {} as it's not a valid number, ignoring", field.getName());
                                }
                            }
                            // noinspection rawtypes
                            case Property property -> {
                                Object value = gson.fromJson(in, property.get().getClass());
                                if (value != null) {
                                    // noinspection unchecked
                                    property.set(value);
                                } else {
                                    WikiRenderer.LOGGER.warn("Found null/invalid value from json for field {}, ignoring", field.getName());
                                }
                            }
                            case null, default -> field.set(bundle, gson.fromJson(in, field.getType()));
                        }
                    } catch (Exception e) {
                        WikiRenderer.LOGGER.error("Failed to parse/set config field {}", field.getName(), e);
                    }
                }
                in.endObject();
                return bundle;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        private List<Field> getAllFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            return fields;
        }
    }
}