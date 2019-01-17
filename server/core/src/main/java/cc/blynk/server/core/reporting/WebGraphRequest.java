package cc.blynk.server.core.reporting;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.Period;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.10.15.
 */
public class WebGraphRequest {

    public final int deviceId;

    public final PinType pinType;

    public final short pin;
    public final AggregationFunctionType functionType;
    public final int count;
    public final GraphGranularityType type;
    public final int skipCount;
    private final Period graphPeriod;
    public long from;

    public long to;

    public WebGraphRequest(int deviceId, DataStream dataStream,
                           Period period, int skipCount,
                           AggregationFunctionType function, long from, long to) {
        this(deviceId, dataStream, period, skipCount, function);
        this.from = from;
        this.to = to;
    }

    private WebGraphRequest(int deviceId, DataStream dataStream,
                            Period period, int skipCount, AggregationFunctionType function) {
        this.deviceId = deviceId;
        if (dataStream == null) {
            this.pinType = PinType.VIRTUAL;
            this.pin = (short) DataStream.NO_PIN;
        } else {
            this.pinType = (dataStream.pinType == null ? PinType.VIRTUAL : dataStream.pinType);
            this.pin = dataStream.pin;
        }
        this.graphPeriod = period;
        this.functionType = (function == null ? AggregationFunctionType.AVG : function);
        this.count = period.numberOfPoints;
        this.type = period.granularityType;
        this.skipCount = skipCount;
    }

    @Override
    public String toString() {
        return "WebGraphRequest{"
                + ", deviceId=" + deviceId
                + ", pinType=" + pinType
                + ", pin=" + pin
                + ", graphPeriod=" + graphPeriod
                + ", functionType=" + functionType
                + ", count=" + count
                + ", type=" + type
                + ", skipCount=" + skipCount
                + '}';
    }
}
