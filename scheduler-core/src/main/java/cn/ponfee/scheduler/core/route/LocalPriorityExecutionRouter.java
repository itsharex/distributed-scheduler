/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.param.ExecuteParam;

import java.util.List;
import java.util.Objects;

/**
 * Local priority execution router.
 *
 * @author Ponfee
 */
public class LocalPriorityExecutionRouter extends ExecutionRouter {

    private final ExecutionRouter otherExecutionRouter;

    public LocalPriorityExecutionRouter(ExecutionRouter otherExecutionRouter) {
        this.otherExecutionRouter = Objects.requireNonNull(otherExecutionRouter);
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.LOCAL_PRIORITY;
    }

    @Override
    protected Worker doRoute(ExecuteParam param, List<Worker> workers) {
        if (Worker.current() != null) {
            return Worker.current();
        }
        return otherExecutionRouter.route(param, workers);
    }

}
