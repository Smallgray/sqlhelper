package com.jn.sqlhelper.dialect.instrument;

import com.jn.langx.annotation.NonNull;
import com.jn.sqlhelper.dialect.sqlparser.SqlStatementWrapper;

public interface ClauseInsturmentor<Statement> {
    void instrument(@NonNull SqlStatementWrapper<Statement> statement, @NonNull InstrumentConfig config);
}
