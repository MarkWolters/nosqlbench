package io.nosqlbench.adapter.cqld4.optypes;

/*
 * Copyright (c) 2022 nosqlbench
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import io.nosqlbench.adapter.cqld4.Cqld4Op;
import io.nosqlbench.adapter.cqld4.Cqld4OpMetrics;
import io.nosqlbench.adapter.cqld4.RSProcessors;

public class Cqld4PreparedStatement extends Cqld4Op {

    private final BoundStatement stmt;

    public Cqld4PreparedStatement(CqlSession session, BoundStatement stmt, int maxpages, boolean retryreplace, Cqld4OpMetrics metrics, RSProcessors processors) {
        super(session,maxpages,retryreplace,metrics,processors);
        this.stmt = stmt;
    }

    public BoundStatement getStmt() {
        return stmt;
    }

    @Override
    public String getQueryString() {
        return stmt.getPreparedStatement().getQuery();
    }
}
