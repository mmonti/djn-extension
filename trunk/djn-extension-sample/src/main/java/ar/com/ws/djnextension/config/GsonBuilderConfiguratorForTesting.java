/*
 * Copyright © 2008, 2009 Pedro Agulló Soliveres.
 * 
 * This file is part of DirectJNgine.
 *
 * DirectJNgine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * Commercial use is permitted to the extent that the code/component(s)
 * do NOT become part of another Open Source or Commercially developed
 * licensed development library or toolkit without explicit permission.
 *
 * DirectJNgine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with DirectJNgine.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * This software uses the ExtJs library (http://extjs.com), which is 
 * distributed under the GPL v3 license (see http://extjs.com/license).
 */

package ar.com.ws.djnextension.config;

import java.lang.reflect.Type;
import java.util.Date;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.softwarementors.extjs.djn.StringUtils;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.gson.DefaultGsonBuilderConfigurator;

/**
 * @author Pedro Agullo Soliveres
 *
 */
public class GsonBuilderConfiguratorForTesting extends DefaultGsonBuilderConfigurator {

	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.gson.DefaultGsonBuilderConfigurator#configure(com.google.gson.GsonBuilder, com.softwarementors.extjs.djn.config.GlobalConfiguration)
	 */
	@Override
	public void configure(GsonBuilder builder, GlobalConfiguration configuration) {
		super.configure(builder, configuration);
		addCustomSerializationSupport(builder);
	}

	/**
	 * @author eM3
	 *
	 */
	private static class JsonDateSerializer implements JsonSerializer<Date> {
		
		@SuppressWarnings("deprecation")
		// Unfortunately Date has many deprecated methods
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
			assert src != null;
			assert context != null;
			assert typeOfSrc != null;

			JsonObject result = new JsonObject();
			setIntValue(result, "year", src.getYear() + 1900); 
			setIntValue(result, "month", src.getMonth() + 1);
			setIntValue(result, "day", src.getDate());

			return result;
		}
	}

	/**
	 * @author eM3
	 *
	 */
	private static class JsonDateDeserializer implements JsonDeserializer<Date> {
	
		@SuppressWarnings("deprecation")
		// Unfortunately Date has many deprecated methods
		// @Override
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			assert json != null;
			assert context != null;
			assert typeOfT != null;

			if (!json.isJsonObject()) {
				throw new JsonParseException("A Date must be a JSON object");
			}

			JsonObject jsonObject = json.getAsJsonObject();
			int year = getIntValue(jsonObject, "year") - 1900;
			int month = getIntValue(jsonObject, "month") - 1;
			int day = getIntValue(jsonObject, "day");

			Date result = new Date(year, month, day);
			return result;
		}
	}

	/**
	 * @param builder
	 */
	private void addCustomSerializationSupport(GsonBuilder builder) {

		// Convert our own custom javascript "date" to a Java Date
		builder.registerTypeAdapter(Date.class, new JsonDateSerializer());

		// Convert a Java Date to our own custom javascript "date"
		builder.registerTypeAdapter(Date.class, new JsonDateDeserializer());
	}

	/**
	 * @param parent
	 * @param elementName
	 * @param value
	 */
	private static void setIntValue(JsonObject parent, String elementName, int value) {
		parent.add(elementName, new JsonPrimitive(Integer.valueOf(value)));
	}

	/**
	 * @param parent
	 * @param elementName
	 * @return
	 */
	private static int getIntValue(JsonObject parent, String elementName) {
		assert parent != null;
		assert !StringUtils.isEmpty(elementName);

		JsonElement element = parent.get(elementName);
		if (!element.isJsonPrimitive()) {
			throw new JsonParseException("Element + '" + elementName + "' must be a valid integer");
		}
		JsonPrimitive primitiveElement = (JsonPrimitive) element;
		if (!primitiveElement.isNumber()) {
			throw new JsonParseException("Element + '" + elementName + "' must be a valid integer");
		}
		return primitiveElement.getAsInt();
	}
}
