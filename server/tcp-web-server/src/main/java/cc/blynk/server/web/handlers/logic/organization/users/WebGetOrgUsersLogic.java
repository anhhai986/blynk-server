package cc.blynk.server.web.handlers.logic.organization.users;

import cc.blynk.server.Holder;
import cc.blynk.server.core.PermissionBasedLogic;
import cc.blynk.server.core.dao.OrganizationDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.web.WebAppStateHolder;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import static cc.blynk.server.core.model.permissions.PermissionsTable.ORG_VIEW_USERS;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.04.18.
 */
public final class WebGetOrgUsersLogic implements PermissionBasedLogic<WebAppStateHolder> {

    private final UserDao userDao;
    private final OrganizationDao organizationDao;

    public WebGetOrgUsersLogic(Holder holder) {
        this.userDao = holder.userDao;
        this.organizationDao = holder.organizationDao;
    }

    @Override
    public int getPermission() {
        return ORG_VIEW_USERS;
    }

    @Override
    public void messageReceived0(ChannelHandlerContext ctx, WebAppStateHolder state, StringMessage message) {
        int requestedOrgId = Integer.parseInt(message.body);
        User user = state.user;

        organizationDao.checkInheritanceAccess(user.email, user.orgId, requestedOrgId);

        if (ctx.channel().isWritable()) {
            List<User> users = userDao.getUsersByOrgId(requestedOrgId, user.email);
            String usersString = JsonParser.toJson(users);
            ctx.writeAndFlush(makeUTF8StringMessage(message.command, message.id, usersString),
                    ctx.voidPromise());
        }
    }

}
