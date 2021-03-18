/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = ViewSummaryDTO.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@WithBeanGetter
public abstract class ViewSummaryDTO implements ContentPackable<ViewEntity.Builder> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SEARCH_ID = "search_id";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

    public static final ImmutableSet<String> SORT_FIELDS = ImmutableSet.of(FIELD_ID, FIELD_TITLE, FIELD_CREATED_AT);

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    public abstract ViewDTO.Type type();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    // A short, one sentence description of the view
    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public static Set<String> idsFrom(Collection<ViewSummaryDTO> views) {
        return views.stream().map(ViewSummaryDTO::id).collect(Collectors.toSet());
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(ViewDTO.Type type);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_SUMMARY)
        public abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_SEARCH_ID)
        public abstract Builder searchId(String searchId);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewSummaryDTO.Builder()
                    .type(ViewDTO.Type.DASHBOARD)
                    .summary("")
                    .description("")
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewSummaryDTO build();
    }

    @Override
    public ViewEntity.Builder toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        final ViewEntity.Builder viewEntityBuilder = ViewEntity.builder()
                .type(this.type())
                .title(ValueReference.of(this.title()))
                .summary(ValueReference.of(this.summary()))
                .description(ValueReference.of(this.description()))
                .createdAt(this.createdAt());

        if (this.owner().isPresent()) {
            viewEntityBuilder.owner(this.owner().get());
        }

        return viewEntityBuilder;
    }
}
