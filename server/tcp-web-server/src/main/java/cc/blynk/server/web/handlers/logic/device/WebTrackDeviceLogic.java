package cc.blynk.server.web.handlers.logic.device;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.web.WebAppStateHolder;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class WebTrackDeviceLogic {

    private static final Logger log = LogManager.getLogger(WebTrackDeviceLogic.class);

    private WebTrackDeviceLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, WebAppStateHolder state, StringMessage message) {
        //todo security check
        int deviceId = Integer.parseInt(message.body);
        state.selectedDeviceId = deviceId;
        log.trace("Selecting webapp device {} for {}.", deviceId, state.user.email);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
