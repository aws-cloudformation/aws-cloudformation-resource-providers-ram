package software.amazon.ram.permission;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PermissionHelperTest {
    @Test
    public void canWriteJsonStringAsObject() {
        final String string = "{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}";
        final Map<String, Object> jsonObject = PermissionHelper.convertToJsonObject("{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}");

        assertEquals("Allow", jsonObject.get("Effect"));
        assertEquals(1, ((List<?>) jsonObject.get("Action")).size());
        assertEquals("foo:bar", ((List<?>) jsonObject.get("Action")).get(0));
    }

    @Test
    public void canWriteObjectAsJsonString() {
        final Map<String, Object> jsonObject = new HashMap<>();
        PermissionHelper.convertToJsonObject("{\"Effect\":\"Allow\",\"Action\":[\"foo:bar\"]}");
        jsonObject.put("Effect", "Allow");
        jsonObject.put("Action", new String[]{"foo:bar"});
        final String expectedString = "{\"Action\":[\"foo:bar\"],\"Effect\":\"Allow\"}";

        assertEquals(expectedString, PermissionHelper.convertJsonObjectToString(jsonObject));
    }
}
