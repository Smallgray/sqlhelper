/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at  http://www.gnu.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jn.sqlhelper.dialect;

import com.jn.langx.annotation.Nullable;
import com.jn.langx.util.Strings;
import com.jn.sqlhelper.common.utils.SQLs;
import com.jn.sqlhelper.dialect.likeescaper.LikeEscaper;
import com.jn.sqlhelper.dialect.orderby.OrderByBuilder;
import com.jn.sqlhelper.dialect.orderby.SqlStyleOrderByBuilder;
import com.jn.sqlhelper.dialect.pagination.PagingRequest;
import com.jn.sqlhelper.dialect.pagination.PagingRequestContextHolder;

public class SqlRequests extends SQLs {

    public static SelectRequest prepareSelect(@Nullable boolean escapeLikeParameter) {
        return prepareSelect(escapeLikeParameter, null, null, null);
    }

    public static SelectRequest prepareSelect(@Nullable Boolean escapeLikeParameter, @Nullable LikeEscaper likeEscaper) {
        return prepareSelect(escapeLikeParameter, likeEscaper, null, null);
    }

    public static SelectRequest prepareSelect(@Nullable Boolean escapeLikeParameter,
                                              @Nullable LikeEscaper likeEscaper,
                                              @Nullable String sort,
                                              @Nullable OrderByBuilder<String> orderByBuilder) {
        return prepareSelect(escapeLikeParameter, likeEscaper, sort, orderByBuilder, null, null, -1);
    }


    public static SelectRequest prepareSelect(@Nullable Boolean escapeLikeParameter,
                                              @Nullable LikeEscaper likeEscaper,
                                              @Nullable String sort,
                                              @Nullable OrderByBuilder<String> orderByBuilder,
                                              @Nullable Integer timeout,
                                              @Nullable Integer fetchSize,
                                              int maxRows) {

        SelectRequest request = new SelectRequest();
        request.setLikeEscaper(likeEscaper).setEscapeLikeParameter(escapeLikeParameter).setFetchSize(fetchSize);
        request.setTimeout(timeout);
        if (maxRows < 0) {
            maxRows = -1;
        }
        request.setMaxRows(maxRows);
        if (Strings.isNotEmpty(sort)) {
            if (orderByBuilder == null) {
                orderByBuilder = SqlStyleOrderByBuilder.DEFAULT;
            }
            request.setOrderBy(orderByBuilder.build(sort));
        }


        return request;
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize) {
        return preparePagination(pageNo, pageSize, null);
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize, String sort) {
        return preparePagination(pageNo, pageSize, sort, null);
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize, String sort, OrderByBuilder<String> orderByBuilder) {
        return preparePagination(pageNo, pageSize, sort, orderByBuilder, true);
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize, String sort, OrderByBuilder<String> orderByBuilder, String dialect) {
        return preparePagination(pageNo, pageSize, sort, orderByBuilder, dialect, true, null);
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize, String sort, OrderByBuilder<String> orderByBuilder, boolean count) {
        return preparePagination(pageNo, pageSize, sort, orderByBuilder, null, count, null);
    }

    public static <C, E> PagingRequest<C, E> preparePagination(int pageNo, int pageSize, String sort, OrderByBuilder<String> orderByBuilder, String dialect, boolean count, String countColumn) {
        PagingRequest<C, E> pagingRequest = new PagingRequest<C, E>().limit(pageNo, pageSize);
        if (Strings.isNotEmpty(sort)) {
            if (orderByBuilder == null) {
                orderByBuilder = SqlStyleOrderByBuilder.DEFAULT;
            }
            pagingRequest.setOrderBy(orderByBuilder.build(sort));
        }
        if (Strings.isNotEmpty(dialect)) {
            pagingRequest.setDialect(dialect);
        }
        pagingRequest.setCount(count);
        if (Strings.isNotEmpty(countColumn)) {
            pagingRequest.setCountColumn(countColumn);
        }

        PagingRequestContextHolder.getContext().setPagingRequest(pagingRequest);
        return pagingRequest;
    }
}
