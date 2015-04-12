/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package keywhiz.service.daos;

import com.google.common.collect.ImmutableMap;
import keywhiz.api.model.Secret;
import keywhiz.service.crypto.ContentCryptographer;
import keywhiz.service.crypto.CryptoFixtures;
import keywhiz.service.crypto.SecretTransformer;

/**
 * Helper methods to make secrets, reducing the amount of work for testing.
 */
public class SecretFixtures {
  private final SecretDAO secretDAO;
  private final SecretJooqDao secretJooqDao;
  private final ContentCryptographer cryptographer;
  private final SecretTransformer transformer;

  private SecretFixtures(SecretDAO secretDAO, SecretJooqDao secretJooqDao) {
    this.secretDAO = secretDAO;
    this.secretJooqDao = secretJooqDao;
    this.cryptographer = CryptoFixtures.contentCryptographer();
    this.transformer = new SecretTransformer(cryptographer);
  }

  /**
   * @return builds a fixture-making object using the given SecretDAO
   */
  public static SecretFixtures using(SecretDAO secretDAO) {
    return new SecretFixtures(secretDAO, null);
  }

  public static SecretFixtures using(SecretJooqDao secretJooqDao) {
    return new SecretFixtures(null, secretJooqDao);
  }

  /**
   * Create a new secret without a version.
   *
   * @param name secret name
   * @param content secret content
   * @return created secret model
   */
  public Secret createSecret(String name, String content) {
    return createSecret(name, content, "");
  }

  /**
   * Create a new secret.
   *
   * @param name secret name
   * @param content secret content
   * @param version secret version
   * @return created secret model
   */
  public Secret createSecret(String name, String content, String version) {
    String encryptedContent = cryptographer.encryptionKeyDerivedFrom(name).encrypt(content);
    if (secretDAO != null) {
      long id =
          secretDAO.createSecret(name, encryptedContent, version, "creator", ImmutableMap.of(), "",
              null, ImmutableMap.of());
      return transformer.transform(secretDAO.getSecretByIdAndVersion(id, version).get());
    } else if (secretJooqDao != null) {
      long id =
          secretJooqDao.createSecret(name, encryptedContent, version, "creator", ImmutableMap.of(),
              "", null, ImmutableMap.of());
      return transformer.transform(secretJooqDao.getSecretByIdAndVersion(id, version).get());
    }
    return null;
  }
}
