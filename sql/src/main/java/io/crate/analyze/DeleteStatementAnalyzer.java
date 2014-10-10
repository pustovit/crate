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

package io.crate.analyze;

import com.google.common.base.Preconditions;
import io.crate.analyze.where.WhereClause;
import io.crate.metadata.TableIdent;
import io.crate.metadata.relation.AnalyzedRelation;
import io.crate.metadata.relation.TableRelation;
import io.crate.metadata.table.TableInfo;
import io.crate.planner.symbol.RelationSymbol;
import io.crate.planner.symbol.Symbol;
import io.crate.sql.tree.Delete;
import io.crate.sql.tree.Table;

public class DeleteStatementAnalyzer extends AbstractStatementAnalyzer<Symbol, DeleteAnalysis> {

    final DataStatementAnalyzer<DeleteAnalysis.NestedDeleteAnalysis> innerAnalyzer = new DataStatementAnalyzer<DeleteAnalysis.NestedDeleteAnalysis>() {
        @Override
        public Symbol visitDelete(Delete node, DeleteAnalysis.NestedDeleteAnalysis context) {
            Symbol relationSymbol = process(node.getRelation(), context);
            assert relationSymbol instanceof RelationSymbol;

            AnalyzedRelation relation = ((RelationSymbol) relationSymbol).relation();
            context.relation(relation);
            if (node.getWhere().isPresent()) {
                Symbol query = process(node.getWhere().get(), context);
                relation.whereClause(new WhereClause(context.normalizer.normalize(query)));
            } else {
                relation.whereClause(WhereClause.MATCH_ALL);
            }

            return null;
        }

        @Override
        protected Symbol visitTable(Table node, DeleteAnalysis.NestedDeleteAnalysis context) {
            Preconditions.checkState(context.relation() == null, "deleting multiple tables is not supported");
            TableIdent tableIdent = TableIdent.of(node);
            TableInfo tableInfo = context.referenceInfos.getEditableTableInfoSafe(tableIdent);
            context.updateRowGranularity(tableInfo.rowGranularity());
            TableRelation tableRelation = new TableRelation(tableInfo, context.partitionResolver());
            context.allocationContext().currentRelation = tableRelation;
            return new RelationSymbol(tableRelation);
        }
    };

    @Override
    public Symbol visitDelete(Delete node, DeleteAnalysis context) {
        java.util.List<DeleteAnalysis.NestedDeleteAnalysis> nestedAnalysis = context.nestedAnalysisList;
        for (int i = 0, nestedAnalysisSize = nestedAnalysis.size(); i < nestedAnalysisSize; i++) {
            DeleteAnalysis.NestedDeleteAnalysis nestedAnalysi = nestedAnalysis.get(i);
            context.parameterContext().setBulkIdx(i);
            innerAnalyzer.process(node, nestedAnalysi);
        }
        return null;
    }
}
