/*
 * Copyright 2024 Steinar Bang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package no.priv.bang.handlelapp.services;

/**
 * Unchecked exception for the handlelapp application
 *
 * Application specific unchecked exception.
 *
 * @author Steinar Bang
 *
 */
public class HandlelappException extends RuntimeException {
    private static final long serialVersionUID = 8126253403392141972L;

    /**
     * {@inheritDoc}
     */
    public HandlelappException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public HandlelappException(Throwable e) {
        super(e);
    }

    /**
     * {@inheritDoc}
     */
    public HandlelappException(String message, Throwable e) {
        super(message, e);
    }

    /**
     * {@inheritDoc}
     */
    public HandlelappException(String message, Throwable e, boolean enableSuppression, boolean writableStackTrace) {
        super(message, e, enableSuppression, writableStackTrace);
    }

}
