/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.executor.transport;

import com.google.common.collect.ImmutableList;
import io.crate.integrationtests.SQLTransportIntegrationTest;
import io.crate.integrationtests.Setup;
import io.crate.metadata.ColumnIdent;
import io.crate.metadata.ReferenceIdent;
import io.crate.metadata.ReferenceInfo;
import io.crate.metadata.TableIdent;
import io.crate.metadata.doc.DocSchemaInfo;
import io.crate.planner.RowGranularity;
import io.crate.planner.node.dql.ESGetNode;
import io.crate.planner.symbol.Reference;
import io.crate.planner.symbol.Symbol;
import io.crate.test.integration.CrateIntegrationTest;
import io.crate.test.integration.CrateTestCluster;
import io.crate.testing.TestingHelpers;
import io.crate.types.DataTypes;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.junit.After;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;

@CrateIntegrationTest.ClusterScope(scope = CrateIntegrationTest.Scope.GLOBAL)
public class BaseTransportExecutorTest extends SQLTransportIntegrationTest {

    Setup setup = new Setup(sqlExecutor);

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
    }

    ClusterService clusterService;
    ClusterName clusterName;
    TransportExecutor executor;
    DocSchemaInfo docSchemaInfo;

    TableIdent charactersIdent = new TableIdent(null, "characters");
    TableIdent booksIdent = new TableIdent(null, "books");

    Reference idRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(charactersIdent, "id"), RowGranularity.DOC, DataTypes.INTEGER));
    Reference nameRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(charactersIdent, "name"), RowGranularity.DOC, DataTypes.STRING));
    Reference femaleRef = TestingHelpers.createReference(charactersIdent.name(), new ColumnIdent("female"), DataTypes.BOOLEAN);
    Reference versionRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(charactersIdent, "_version"), RowGranularity.DOC, DataTypes.LONG));

    Reference booksIdRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(booksIdent, "id"), RowGranularity.DOC, DataTypes.INTEGER));
    Reference titleRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(booksIdent, "title"), RowGranularity.DOC, DataTypes.STRING));
    Reference authorRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(booksIdent, "author"), RowGranularity.DOC, DataTypes.STRING));

    TableIdent partedTable = new TableIdent(null, "parted");
    Reference partedIdRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(partedTable, "id"), RowGranularity.DOC, DataTypes.INTEGER));
    Reference partedNameRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(partedTable, "name"), RowGranularity.DOC, DataTypes.STRING));
    Reference partedDateRef = new Reference(new ReferenceInfo(
            new ReferenceIdent(partedTable, "date"), RowGranularity.DOC, DataTypes.TIMESTAMP));

    protected static ESGetNode newGetNode(String index, List<Symbol> outputs, String id) {
        return newGetNode(index, outputs, Arrays.asList(id));
    }

    protected static ESGetNode newGetNode(String index, List<Symbol> outputs, List<String> ids) {
        return new ESGetNode(
                index,
                outputs,
                ids,
                ids,
                ImmutableList.<Symbol>of(),
                new boolean[0],
                new Boolean[0],
                null,
                0,
                null
        );
    }

    @Before
    public void transportSetUp() {
        CrateTestCluster cluster = cluster();
        executor = cluster.getInstance(TransportExecutor.class);

        docSchemaInfo = cluster.getInstance(DocSchemaInfo.class);
    }

    @After
    public void transportTearDown() {
        executor = null;
        docSchemaInfo = null;
    }
}
