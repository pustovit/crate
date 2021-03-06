/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.operation.reference.sys.node;

import io.crate.operation.reference.sys.SysNodeObjectReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.monitor.os.OsService;

public class NodeLoadExpression extends SysNodeObjectReference {

    public static final String NAME = "load";

    public static final String ONE = "1";
    public static final String FIVE = "5";
    public static final String FIFTEEN = "15";

    private final OsService osService;

    @Inject
    public NodeLoadExpression(OsService osService) {
        this.osService = osService;
        childImplementations.put(ONE, new LoadExpression(0));
        childImplementations.put(FIVE, new LoadExpression(1));
        childImplementations.put(FIFTEEN, new LoadExpression(2));
    }

    class LoadExpression extends SysNodeExpression<Double> {

        private final int idx;

        LoadExpression(int idx) {
            this.idx = idx;
        }

        @Override
        public Double value() {
            try {
                return osService.stats().loadAverage()[idx];
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

}
