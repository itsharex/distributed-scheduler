package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.base.Constants;
import lombok.Getter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * The abstract server class definition.
 *
 * @author Ponfee
 */
@Getter
public abstract class Server implements Serializable {

    private static final long serialVersionUID = -783308216490505598L;

    /**
     * Server host
     */
    protected final String host;

    /**
     * Port number
     */
    protected final int port;

    public Server(String host, int port) {
        Assert.isTrue(!host.contains(Constants.COLON), "Host cannot contains symbol ':'");
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }

        Server other = (Server) o;
        return Objects.equals(host, other.host)
            && port == other.port;
    }

    /**
     * Extends {@code Object#hashCode()}
     *
     * @return a hash code value for this object.
     */
    @Override
    public abstract int hashCode();

    /**
     * Serialize to string
     *
     * @return string of the worker serialized result
     */
    public abstract String serialize();

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     * @see #serialize()
     */
    @Override
    public final String toString() {
        return serialize();
    }

}
