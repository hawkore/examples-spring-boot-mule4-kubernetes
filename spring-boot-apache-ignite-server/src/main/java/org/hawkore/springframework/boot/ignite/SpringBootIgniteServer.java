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
package org.hawkore.springframework.boot.ignite;

import com.hawkore.license.activator.EnableHKLicenseManagement;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBootIgniteServer
 *
 * @author Manuel Núñez Sánchez (manuel.nunez@hawkore.com)
 */
@SpringBootApplication
// this annotation will expose LOCAL HK License management endpoints to install licenses for HK Products
@EnableHKLicenseManagement
public class SpringBootIgniteServer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringBootIgniteServer.class);
        app.setBannerMode(Mode.OFF);
        app.run(args);
    }

}
