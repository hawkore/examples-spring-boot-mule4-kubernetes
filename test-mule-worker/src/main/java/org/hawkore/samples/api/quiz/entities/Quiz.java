/*
 * Copyright 2020 HAWKORE, S.L.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkore.samples.api.quiz.entities;

import java.io.Serializable;
import java.util.StringJoiner;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 * Quiz, convenient annotated fields for SQL queries (distributed database)
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
public class Quiz implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * Surveyed's email
     */
    @QuerySqlField(index = true, inlineSize = 200)
    private String email;
    /**
     * YES Response
     */
    @QuerySqlField(index = true)
    private boolean yes;
    /**
     * NO Response
     */
    @QuerySqlField(index = true)
    private boolean no;
    /**
     * Non Answered Response
     */
    @QuerySqlField(index = true)
    private boolean na;
    /**
     * Quiz creation timestamp
     */
    @QuerySqlField(index = true)
    private long qts;
    /**
     * Quiz process timestamp
     */
    @QuerySqlField(index = true)
    private long pts;
    /**
     * API node IP that received Quiz
     */
    @QuerySqlField(index = true)
    private String apiIp;
    /**
     * Worker node IP that processed Quiz
     */
    @QuerySqlField(index = true)
    private String workerIp;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isYes() {
        return yes;
    }

    public void setYes(boolean yes) {
        this.yes = yes;
    }

    public boolean isNo() {
        return no;
    }

    public void setNo(boolean no) {
        this.no = no;
    }

    public boolean isNa() {
        return na;
    }

    public void setNa(boolean na) {
        this.na = na;
    }

    public long getQts() {
        return qts;
    }

    public void setQts(long qts) {
        this.qts = qts;
    }

    public long getPts() {
        return pts;
    }

    public void setPts(long pts) {
        this.pts = pts;
    }

    public String getWorkerIp() {
        return workerIp;
    }

    public void setWorkerIp(String workerIp) {
        this.workerIp = workerIp;
    }

    public String getApiIp() {
        return apiIp;
    }

    public void setApiIp(String apiIp) {
        this.apiIp = apiIp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Quiz.class.getSimpleName() + "[", "]").add("email='" + email + "'")
                   .add("yes=" + yes).add("no=" + no).add("na=" + na).add("qts=" + qts).add("pts=" + pts)
                   .add("apiIp='" + apiIp + "'").add("workerIp='" + workerIp + "'").toString();
    }

}
