package cc.blynk.integration.web;

import cc.blynk.integration.SingleServerInstancePerTestWithDBAndNewOrg;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.integration.model.websocket.AppWebSocketClient;
import cc.blynk.server.common.handlers.logic.timeline.TimelineResponseDTO;
import cc.blynk.server.core.model.device.ConnectionType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.dto.ProductDTO;
import cc.blynk.server.core.model.web.product.EventReceiver;
import cc.blynk.server.core.model.web.product.EventType;
import cc.blynk.server.core.model.web.product.MetaField;
import cc.blynk.server.core.model.web.product.MetadataType;
import cc.blynk.server.core.model.web.product.Product;
import cc.blynk.server.core.model.web.product.events.Event;
import cc.blynk.server.core.model.web.product.events.system.OfflineEvent;
import cc.blynk.server.core.model.web.product.events.system.OnlineEvent;
import cc.blynk.server.core.model.web.product.events.user.CriticalEvent;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.dao.descriptor.LogEventDTO;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.utils.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.integration.APIBaseTest.createContactMeta;
import static cc.blynk.integration.APIBaseTest.createDeviceNameMeta;
import static cc.blynk.integration.APIBaseTest.createDeviceOwnerMeta;
import static cc.blynk.integration.APIBaseTest.createNumberMeta;
import static cc.blynk.integration.APIBaseTest.createTextMeta;
import static cc.blynk.integration.TestUtil.TEN_YEARS_MILLIS;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.deviceConnected;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.getDefaultHttpsClient;
import static cc.blynk.integration.TestUtil.logEvent;
import static cc.blynk.integration.TestUtil.loggedDefaultClient;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.sleep;
import static cc.blynk.server.core.protocol.enums.Command.WEB_RESOLVE_EVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class LogEventTcpAndHttpAPIWebsocketTest extends SingleServerInstancePerTestWithDBAndNewOrg {

    @Before
    public void clearDB() throws Exception {
        //clean everything just in case
        holder.reportingDBManager.executeSQL("ALTER TABLE reporting_events delete where device_id > 0");
        holder.reportingDBManager.executeSQL("ALTER TABLE reporting_events_resolved delete where id > 0");
        holder.reportingDBManager.executeSQL("ALTER TABLE reporting_events_last_seen delete where device_id > 0");
    }

    @Test
    public void testBasicLogEventFlow() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.send("logEvent temp_is_high");
        newHardClient.verifyResult(ok(2));
        client.verifyResult(deviceConnected(1, device.id));
        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(timeLineResponse.eventList);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("This is my description", logEvents.get(0).description);
    }

    @Test
    public void testCorruptedLogEvent() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high", "123" + StringUtils.BODY_SEPARATOR_STRING);
        newHardClient.verifyResult(ok(2));
        client.verifyResult(deviceConnected(1, device.id));
        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(timeLineResponse.eventList);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("123", logEvents.get(0).description);
    }

    @Test
    public void testLogEventIsForwardedToWebapp() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);
        client.trackDevice(device.id);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        client.verifyResult(logEvent(1, device.id + " " + EventType.ONLINE.name()));
        client.reset();

        newHardClient.logEvent("temp_is_high");
        newHardClient.verifyResult(ok(2));

        client.verifyResult(logEvent(2, device.id + " CRITICAL temp_is_high"));

        newHardClient.logEvent("temp_is_high", "222");
        newHardClient.verifyResult(ok(3));

        client.verifyResult(logEvent(3, device.id + " CRITICAL temp_is_high 222"));
    }

    private static Device createProductAndDevice(AppWebSocketClient client, int orgId) throws Exception {
        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.metaFields = new MetaField[] {
                createNumberMeta(1, "Jopa", 123D),
                createDeviceOwnerMeta(2, "owner", "owner", true),
                createDeviceNameMeta(3, "anme", "name", true),
                createContactMeta(6, "Farm of Smith"),
                createTextMeta(7, "Device Owner", "owner@blynk.cc")
        };

        Event criticalEvent = new CriticalEvent(
                1,
                "Temp is super high",
                "This is my description",
                true,
                "temp_is_high" ,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith"),
                        new EventReceiver(7, MetadataType.Text, "Device Owner"),
                },
                new EventReceiver[] {
                        new EventReceiver(2, MetadataType.Contact, "Farm of Smith"),
                },
                null
        );

        Event onlineEvent = new OnlineEvent(
                2,
                "Device is online!",
                null,
                true,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null
        );

        Event offlineEvent = new OfflineEvent(
                3,
                "Device is offline!",
                null,
                true,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null,
                0
        );

        Event neverWorkingEvent = new CriticalEvent(
                1,
                "NeverWorkingEvent",
                "This is my description",
                false,
                "never" ,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith"),
                        new EventReceiver(7, MetadataType.Text, "Device Owner")
                },
                null,
                null
        );

        product.events = new Event[] {
                criticalEvent,
                onlineEvent,
                offlineEvent,
                neverWorkingEvent
        };

        client.createProduct(product);
        ProductDTO fromApiProduct = client.parseProductDTO(1);
        assertNotNull(fromApiProduct);

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = fromApiProduct.id;

        client.createDevice(orgId, newDevice);
        Device device = client.parseDevice(2);

        client.trackDevice(device.id);
        client.verifyResult(ok(3));

        client.reset();
        return device;
    }

    @Test
    public void testLogEventIsResolvedFromWebapp() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        String externalApiUrl = String.format("https://localhost:%s/external/api/", properties.getHttpsPort());

        CloseableHttpClient httpclient = getDefaultHttpsClient();
        HttpGet insertEvent = new HttpGet(externalApiUrl + device.token + "/logEvent?code=temp_is_high");
        try (CloseableHttpResponse response = httpclient.execute(insertEvent)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        long now = System.currentTimeMillis();

        long logEventId;
        client.reset();
        client.getTimeline(device.id, EventType.CRITICAL, false, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertNotNull(timeLineResponse);
        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());

        LogEventDTO logEventDTO = logEvents.get(0);
        assertEquals(device.id, logEventDTO.deviceId);
        assertEquals(EventType.CRITICAL, logEventDTO.eventType);
        assertFalse(logEventDTO.isResolved);
        assertEquals("Temp is super high", logEventDTO.name);
        assertEquals("This is my description", logEventDTO.description);
        logEventId = logEventDTO.id;

        client.resolveEvent(device.id, logEventId, "resolve comment");
        client.verifyResult(ok(2));

        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, System.currentTimeMillis(), 0, 10);
        timeLineResponse = client.parseTimelineResponse(3);
        assertNotNull(timeLineResponse);

        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(1, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());

        logEventDTO = logEvents.get(0);
        assertEquals(device.id, logEventDTO.deviceId);
        assertEquals(EventType.CRITICAL, logEventDTO.eventType);
        assertTrue(logEventDTO.isResolved);
        assertEquals("Temp is super high", logEventDTO.name);
        assertEquals("This is my description", logEventDTO.description);
        assertEquals("resolve comment", logEventDTO.resolvedComment);
        assertEquals(logEventId, logEventDTO.id);
    }

    @Test
    public void testLogEventIsForwardedToWebappFromExternalAPI() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);
        client.trackDevice(device.id);
        client.verifyResult(ok(1));

        String externalApiUrl = String.format("https://localhost:%s/external/api/", properties.getHttpsPort());

        CloseableHttpClient httpclient = getDefaultHttpsClient();
        HttpGet insertEvent = new HttpGet(externalApiUrl + device.token + "/logEvent?code=temp_is_high");
        try (CloseableHttpResponse response = httpclient.execute(insertEvent)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        client.verifyResult(logEvent(111, device.id + " CRITICAL temp_is_high"));
    }

    @Test
    public void testLogEventIsResolvedFromWebappAndForwardedToAnotherWebapp() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        String externalApiUrl = String.format("https://localhost:%s/external/api/", properties.getHttpsPort());

        CloseableHttpClient httpclient = getDefaultHttpsClient();
        HttpGet insertEvent = new HttpGet(externalApiUrl + device.token + "/logEvent?code=temp_is_high");
        try (CloseableHttpResponse response = httpclient.execute(insertEvent)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        long now = System.currentTimeMillis();

        long logEventId;
        client.reset();
        client.getTimeline(device.id, EventType.CRITICAL, false, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        LogEventDTO logEventDTO = logEvents.get(0);
        logEventId = logEventDTO.id;

        AppWebSocketClient client2 = loggedDefaultClient(getUserName(), "1");
        client2.trackDevice(device.id);
        client2.verifyResult(ok(1));

        client.resolveEvent(device.id, logEventId, "resolve comment");
        client.verifyResult(ok(2));

        client2.verifyResult(new StringMessage(2, WEB_RESOLVE_EVENT,
                b(device.id + " " + logEventId + " " + getUserName() + " ") + "resolve comment"));

        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, System.currentTimeMillis(), 0, 10);
        timeLineResponse = client.parseTimelineResponse(3);
        assertNotNull(timeLineResponse);

        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(1, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());

        logEventDTO = logEvents.get(0);
        assertEquals(device.id, logEventDTO.deviceId);
        assertEquals(EventType.CRITICAL, logEventDTO.eventType);
        assertTrue(logEventDTO.isResolved);
        assertEquals("Temp is super high", logEventDTO.name);
        assertEquals("This is my description", logEventDTO.description);
        assertEquals("resolve comment", logEventDTO.resolvedComment);
        assertEquals(logEventId, logEventDTO.id);
    }

    @Test
    public void testSystemLogEventViaHttpApi() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        String externalApiUrl = String.format("https://localhost:%s/external/api/", properties.getHttpsPort());

        CloseableHttpClient httpclient = getDefaultHttpsClient();
        HttpGet insertEvent = new HttpGet(externalApiUrl + device.token + "/logEvent?code=ONLINE");
        try (CloseableHttpResponse response = httpclient.execute(insertEvent)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.ONLINE, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.ONLINE, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Device is online!", logEvents.get(0).name);
    }

    @Test
    public void testBasicLogEventWithOverrideDescriptionFlow() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high", "MyNewDescription");
        newHardClient.verifyResult(ok(2));
        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("MyNewDescription", logEvents.get(0).description);
    }


    @Test
    // https://github.com/blynkkk/dash/issues/1996
    public void testLogEventNotificationBody() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);


        TestAppClient appClient = new TestAppClient("localhost", properties.getHttpsPort());
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "2.27.1");
        appClient.verifyResult(ok(1));
        appClient.addPushToken("uid", "token");
        appClient.verifyResult(ok(2));


        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high", "MyNewDescription");
        newHardClient.verifyResult(ok(2));
        client.reset();


        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);

        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("MyNewDescription", logEvents.get(0).description);


        String expected = "My New Device:\nTemp is super high\nMyNewDescription";

        verify(holder.gcmWrapper, timeout(1000)).sendAndroid(
                any((Class<ConcurrentHashMap<String, String>>)(Class) ConcurrentHashMap.class), any(Priority.class),
                eq(expected), any(Integer.class));

        verify(holder.gcmWrapper, timeout(1000)).sendIOS(
                any((Class<ConcurrentHashMap<String, String>>)(Class) ConcurrentHashMap.class), any(Priority.class),
                eq(expected), any(Integer.class));
    }

    @Test
    public void testEmailNotificationWorkWithEvent() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high", "MyNewDescription");
        newHardClient.verifyResult(ok(2));
        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("MyNewDescription", logEvents.get(0).description);

        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq("dmitriy@blynk.cc"), eq("My New Device: Temp is super high"), contains("Temp is super high"));
        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq("owner@blynk.cc"), eq("My New Device: Temp is super high"), contains("Temp is super high"));
    }

    @Test
    public void testEmailNotificationWorkWithEventViaHttpsAPI() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.reset();

        String externalApiUrl = String.format("https://localhost:%s/external/api/", properties.getHttpsPort());

        CloseableHttpClient httpclient = getDefaultHttpsClient();
        HttpGet insertEvent = new HttpGet(externalApiUrl + device.token + "/logEvent?code=temp_is_high&description=MyNewDescription");
        try (CloseableHttpResponse response = httpclient.execute(insertEvent)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        client.reset();
        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.CRITICAL, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
        assertEquals("Temp is super high", logEvents.get(0).name);
        assertEquals("MyNewDescription", logEvents.get(0).description);

        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq("dmitriy@blynk.cc"), eq("My New Device: Temp is super high"), contains("Temp is super high"));
        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq("owner@blynk.cc"), eq("My New Device: Temp is super high"), contains("Temp is super high"));
    }

    @Test
    public void testOnlineOfflineLogEventIsForwardedToWebapp() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);
        client.trackDevice(device.id);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        client.verifyResult(logEvent(1, device.id + " " + EventType.ONLINE.name()));
        client.reset();

        newHardClient.stop();
        client.verifyResult(deviceOffline(0, device.id));
        client.verifyResult(logEvent(0, device.id + " " + EventType.OFFLINE.name()));
    }

    @Test
    public void testBasicLogEventWithIsResolvedFlow() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high", "MyNewDescription");
        newHardClient.verifyResult(ok(2));
        client.reset();

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, true, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        assertNull(timeLineResponse.eventList);
    }

    @Test
    public void testOnlineOfflineLogEventIsNotForwardedToWebappBecauseNoTrack() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Product product = new Product();
        product.name = "My product";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.metaFields = new MetaField[] {
                createNumberMeta(1, "Jopa", 123D),
                createDeviceOwnerMeta(2, "owner", "owner", true),
                createDeviceNameMeta(3, "anme", "name", true),
                createContactMeta(6, "Farm of Smith"),
                createTextMeta(7, "Device Owner", "owner@blynk.cc")
        };

        Event onlineEvent = new OnlineEvent(
                2,
                "Device is online!",
                null,
                false,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null
        );

        Event offlineEvent = new OfflineEvent(
                3,
                "Device is offline!",
                null,
                false,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null,
                0
        );

        product.events = new Event[] {
                onlineEvent,
                offlineEvent
        };

        client.createProduct(product);
        ProductDTO fromApiProduct = client.parseProductDTO(1);
        assertNotNull(fromApiProduct);

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = fromApiProduct.id;

        client.createDevice(orgId, newDevice);
        Device device = client.parseDevice(2);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        client.never(logEvent(1, device.id + " " + EventType.ONLINE.name()));
        client.reset();

        newHardClient.stop();
        client.verifyResult(deviceOffline(0, device.id));
        client.never(logEvent(0, device.id + " " + EventType.OFFLINE.name()));
    }

    @Test
    public void testResolveLogEventFlow() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        newHardClient.logEvent("temp_is_high");
        newHardClient.verifyResult(ok(2));
        client.reset();

        long logEventId;
        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(1, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertTrue(logEvents.get(0).id > 0);
        logEventId = logEvents.get(0).id;

        client.trackDevice(device.id);
        client.verifyResult(ok(2));

        client.resolveEvent(device.id, logEventId, "123");
        client.verifyResult(ok(3));

        now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, true, now - TEN_YEARS_MILLIS, now, 0, 10);
        timeLineResponse = client.parseTimelineResponse(4);
        assertNotNull(timeLineResponse);

        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(1, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        long oldLogEventId = logEvents.get(0).id;

        assertEquals(logEventId, oldLogEventId);
        assertTrue(logEvents.get(0).isResolved);

        assertEquals(System.currentTimeMillis(), logEvents.get(0).resolvedAt, 5000);
        assertEquals("123", logEvents.get(0).resolvedComment);

        now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, false, now - TEN_YEARS_MILLIS, now, 0, 10);
        timeLineResponse = client.parseTimelineResponse(5);
        assertNotNull(timeLineResponse);

        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(1, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNull(logEvents);
    }

    @Test
    public void testOrderByResolveAtAndThanByTs() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.reset();

        newHardClient.logEvent("temp_is_high");
        newHardClient.verifyResult(ok(2));

        newHardClient.logEvent("temp_is_high");
        newHardClient.verifyResult(ok(3));

        newHardClient.logEvent("temp_is_high");
        newHardClient.verifyResult(ok(4));

        client.reset();
        long now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);

        assertEquals(3, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(3, logEvents.size());
        long logEventId = logEvents.get(2).id;

        //todo in real use cases events will never be resolved in the same second when they are retrieved
        //so we don't need to fix this test.
        //clickhouse doesn't store millis, so when this test is executed all time is within 1 seconds
        //and returned result is wrong. so we fixing it with simple sleep
        sleep(1000);

        client.resolveEvent(device.id, logEventId);
        client.verifyResult(ok(2));

        now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.CRITICAL, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        timeLineResponse = client.parseTimelineResponse(3);
        assertNotNull(timeLineResponse);

        assertEquals(2, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(1, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNotNull(logEvents);
        assertEquals(3, logEvents.size());
        assertEquals(logEventId, logEvents.get(0).id);
        assertTrue(logEvents.get(0).isResolved);
        assertEquals(System.currentTimeMillis(), logEvents.get(0).resolvedAt, 5000);
        assertNull(logEvents.get(0).resolvedComment);
    }

    @Test
    public void testSystemEventsCreated() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        client.reset();

        sleep(500);

        long now = System.currentTimeMillis();
        client.getTimeline(device.id, null, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(timeLineResponse.eventList);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.ONLINE, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);

        newHardClient.stop();
        //we have to wait until DB query will be executed and ignore period will pass. it is 1000 millis.
        sleep(1100);
        client.reset();

        now = System.currentTimeMillis();
        client.getTimeline(device.id, EventType.OFFLINE, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        logEvents = timeLineResponse.eventList;
        assertNotNull(timeLineResponse.eventList);
        assertEquals(1, logEvents.size());
        assertEquals(device.id, logEvents.get(0).deviceId);
        assertEquals(EventType.OFFLINE, logEvents.get(0).eventType);
        assertFalse(logEvents.get(0).isResolved);
    }

    //todo finish those tests
    /*
    @Test
    public void testBasicLogEventFlowWithLastView() throws Exception {
        String token = createProductAndDevice();

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.login(token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        newHardClient.send("logEvent temp_is_high");
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        long now = System.currentTimeMillis();

        HttpGet getDevices = new HttpGet(httpsAdminServerUrl + "/devices/1");
        try (CloseableHttpResponse response = httpclient.execute(getDevices)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            DeviceDTO[] devices = readDevices(responseString);
            assertNotNull(devices);
            assertEquals(2, devices.length);
            DeviceDTO device = devices[1];
            assertEquals(1, device.id);
            assertNotNull(device.criticalSinceLastView);
            assertEquals(1, device.criticalSinceLastView.intValue());
            assertNull(device.warningSinceLastView);
        }


    }

    @Test
    public void testBasicLogEventFlowWithLastViewFor2Users() throws Exception {
        String token = createProductAndDevice();

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.login(token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        newHardClient.send("logEvent temp_is_high");
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        long now = System.currentTimeMillis();

        HttpGet getDevices = new HttpGet(httpsAdminServerUrl + "/devices/1");
        try (CloseableHttpResponse response = httpclient.execute(getDevices)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            DeviceDTO[] devices = readDevices(responseString);
            assertNotNull(devices);
            assertEquals(2, devices.length);
            DeviceDTO device = devices[1];
            assertEquals(1, device.id);
            assertNotNull(device.criticalSinceLastView);
            assertEquals(1, device.criticalSinceLastView.intValue());
            assertNull(device.warningSinceLastView);
        }

        HttpGet getEvents = new HttpGet(httpsAdminServerUrl + "/devices/1/1/timeline?eventType=CRITICAL&from=0&to=" + now + "&limit=10&offset=0");
        try (CloseableHttpResponse response = httpclient.execute(getEvents)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            TimeLineResponse timeLineResponse = JsonParser.readAny(responseString, TimeLineResponse.class);
            assertNotNull(timeLineResponse);
            assertEquals(1, timeLineResponse.totalCritical);
            assertEquals(0, timeLineResponse.totalWarning);
            assertEquals(0, timeLineResponse.totalResolved);
            LogEvent[] logEvents = timeLineResponse.logEvents;
            assertNotNull(timeLineResponse.logEvents);
            assertEquals(1, logEvents.length);
            assertEquals(1, logEvents[0].deviceId);
            assertEquals(EventType.CRITICAL, logEvents[0].eventType);
            assertFalse(logEvents[0].isResolved);
            assertEquals("Temp is super high", logEvents[0].name);
            assertEquals("This is my description", logEvents[0].description);
        }

        getDevices = new HttpGet(httpsAdminServerUrl + "/devices/1");
        try (CloseableHttpResponse response = httpclient.execute(getDevices)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            DeviceDTO[] devices = readDevices(responseString);
            assertNotNull(devices);
            assertEquals(2, devices.length);
            DeviceDTO device = devices[1];
            assertEquals(1, device.id);
            assertNull(device.criticalSinceLastView);
            assertNull(device.warningSinceLastView);
        }

        CloseableHttpClient newHttpClient = getDefaultHttpsClient();

        login(newHttpClient,  httpsAdminServerUrl, regularAdmin.email, regularAdmin.pass);

        getDevices = new HttpGet(httpsAdminServerUrl + "/devices/1");
        try (CloseableHttpResponse response = newHttpClient.execute(getDevices)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            DeviceDTO[] devices = readDevices(responseString);
            assertNotNull(devices);
            assertEquals(2, devices.length);
            DeviceDTO device = devices[1];
            assertEquals(1, device.id);
            assertNotNull(device.criticalSinceLastView);
            assertEquals(1, device.criticalSinceLastView.intValue());
            assertNull(device.warningSinceLastView);
        }
    }
    */

    @Test
    //https://github.com/blynkkk/dash/issues/1276
    public void testDeviceQuicklyReconnects() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");
        Device device = createProductAndDevice(client, orgId);

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        newHardClient.stop();
        client.reset();

        newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        newHardClient.stop();
        client.reset();

        //we have to wait for DB.
        sleep(500);

        client.reset();
        long now = System.currentTimeMillis();
        client.getTimeline(device.id, null, null, now - TEN_YEARS_MILLIS, now, 0, 10);
        TimelineResponseDTO timeLineResponse = client.parseTimelineResponse(1);
        assertNotNull(timeLineResponse);
        assertEquals(0, timeLineResponse.totalCritical);
        assertEquals(0, timeLineResponse.totalWarning);
        assertEquals(0, timeLineResponse.totalResolved);
        List<LogEventDTO> logEvents = timeLineResponse.eventList;
        assertNotNull(timeLineResponse.eventList);
        assertEquals(4, logEvents.size());
        assertEquals(EventType.OFFLINE, logEvents.get(0).eventType);
        assertEquals(EventType.ONLINE, logEvents.get(1).eventType);
        assertEquals(EventType.OFFLINE, logEvents.get(2).eventType);
        assertEquals(EventType.ONLINE, logEvents.get(3).eventType);
    }

    @Test
    public void testIgnoreOfflinePeriodWorksAsExpected() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.metaFields = new MetaField[] {
                createNumberMeta(1, "Jopa", 123D),
                createDeviceOwnerMeta(2, "owner", "owner", true),
                createDeviceNameMeta(3, "anme", "name", true),
        };

        Event offlineEvent = new OfflineEvent(
                3,
                "Device is offline!",
                null,
                true,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null,
                500 //ignore offline period for 1 seconds
        );

        product.events = new Event[] {
                offlineEvent
        };

        client.createProduct(product);
        ProductDTO fromApiProduct = client.parseProductDTO(1);
        assertNotNull(fromApiProduct);

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = fromApiProduct.id;

        client.createDevice(orgId, newDevice);
        Device device = client.parseDevice(2);

        client.trackDevice(device.id);
        client.verifyResult(ok(3));
        client.reset();

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));
        client.reset();

        newHardClient.stop();

        //waiting for 250 millis and no response, as ignore period if 500 millis
        client.neverAfter(250, logEvent(0, device.id + " OFFLINE"));

        sleep(260);

        client.verifyResult(logEvent(0, device.id + " OFFLINE"));
    }

    @Test
    public void testIgnoreOfflinePeriodWorksAsExpectedWithDeviceReconnection() throws Exception {
        AppWebSocketClient client = loggedDefaultClient(getUserName(), "1");

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.metaFields = new MetaField[] {
                createNumberMeta(1, "Jopa", 123D),
                createDeviceOwnerMeta(2, "owner", "owner", true),
                createDeviceNameMeta(3, "anme", "name", true),
        };

        Event offlineEvent = new OfflineEvent(
                3,
                "Device is offline!",
                null,
                true,
                new EventReceiver[] {
                        new EventReceiver(6, MetadataType.Contact, "Farm of Smith")
                },
                null,
                null,
                500 //ignore offline period for 1 seconds
        );

        product.events = new Event[] {
                offlineEvent
        };

        client.createProduct(product);
        ProductDTO fromApiProduct = client.parseProductDTO(1);
        assertNotNull(fromApiProduct);

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = fromApiProduct.id;

        client.createDevice(orgId, newDevice);
        Device device = client.parseDevice(2);

        client.trackDevice(device.id);
        client.verifyResult(ok(3));

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        client.verifyResult(deviceConnected(1, device.id));

        newHardClient.stop();

        // value in interval between 0 and ignorePeriod (500)
        sleep(100);

        client.never(logEvent(0, device.id + " OFFLINE"));
        client.reset();

        newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));

        client.verifyResult(deviceConnected(1, device.id));

        // there should not be OFFLINE event during ignore period (500) after reconnection
        sleep(450);
        client.never(logEvent(0, device.id + " OFFLINE"));
    }

}
