/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.resource;

import org.openmrs.OpenmrsData;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.entity.IEntityDataService;
import org.openmrs.module.openhmis.commons.api.entity.IObjectDataService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.*;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseRestDataResource<E extends OpenmrsData> extends DataDelegatingCrudResource<E> implements IEntityDataServiceResource<E> {

	@Override
	public abstract E newDelegate();

	
	@Override
	public E save(E delegate) {
		IEntityDataService<E> service = Context.getService(getServiceClass());
		service.save(delegate);
		return delegate;
	}

	protected DelegatingResourceDescription getDefaultRepresentationDescription() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("display", findMethod("getDisplayString"));
		description.addProperty("voided");
		description.addProperty("voidReason");
		return description;
	}

	@Override
	public DelegatingResourceDescription getRepresentationDescription(
			Representation rep) {
		DelegatingResourceDescription description;
		if (rep instanceof RefRepresentation) {
			description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display", findMethod("getDisplayString"));
			return description;
		}
		description = getDefaultRepresentationDescription();
		if (rep instanceof FullRepresentation)
			description.addProperty("auditInfo", findMethod("getAuditInfo"));
		return description;
	}

	public String getDisplayString(E instance) {
		return instance.getClass().getSimpleName();
	}

	@Override
	public E getByUniqueId(String uniqueId) {
		IObjectDataService<E> service = Context.getService(getServiceClass());
		E entity = service.getByUuid(uniqueId);
		return entity;
	}

	@Override
	protected void delete(E delegate, String reason, RequestContext context)
			throws ResponseException {
		IEntityDataService<E> service = Context.getService(getServiceClass());
		service.voidEntity(delegate, reason);
	}

	@Override
	public void purge(E delegate, RequestContext context)
			throws ResponseException {
		IEntityDataService<E> service = Context.getService(getServiceClass());
		service.purge(delegate);		
	}
	
	@Override
	protected NeedsPaging<E> doGetAll(RequestContext context) throws ResponseException {
		IEntityDataService<E> service = Context.getService(getServiceClass());
		return new NeedsPaging<E>(service.getAll(), context);
	}

	@Override
	protected AlreadyPaged<E> doSearch(String query, RequestContext context) {
		return new ServiceSearcher<E>(this.getServiceClass(), "getResources", "getCountOfResources").search(query,
               context);
	}
	
	/**
	 * Update a collection according to another collection
	 * 
	 * **WARNING**: Side effects: modifies first collection
	 * @param collection
	 * @param update
	 */
	public static <E extends OpenmrsObject> void updateCollection(Collection<E> collection, Collection<E> update) {
		Map<String, E> collectionMap = new HashMap<String, E>();
		Map<String, E> updateMap = new HashMap<String, E>();
		for (E item : collection)
			collectionMap.put(item.getUuid(), item);
		for (E item : update)
			updateMap.put(item.getUuid(), item);
		// First compare update to existing collection
		for (E item : collectionMap.values()) {
			// Update existing items
			if (updateMap.containsKey(item.getUuid())) {
				E updateObj = updateMap.get(item.getUuid());
				updateObj.setId(item.getId());
				BeanUtils.copyProperties(updateObj, item);
			} else // Remove existing items that do not appear in the update
				collection.remove(item);
		}
		// Second add any new items
		for (E item : updateMap.values()) {
			if (!collectionMap.containsKey(item.getUuid()))
				collection.add(item);
		}
	}
	
	public static <E extends OpenmrsObject> Map<String, E> mapUuidToObject(Collection<E> collection) {
		Map<String, E> map = new HashMap<String, E>(collection.size());
		for (E item : collection)
			map.put(item.getUuid(), item);
		return map;
	}
}
