/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.clustered.common.internal.messages;

import org.ehcache.clustered.common.internal.messages.PassiveReplicationMessage.ClientIDTrackerMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

public class EhcacheCodecTest {

  private static final UUID CLIENT_ID = UUID.randomUUID();

  @Mock
  private ServerStoreOpCodec serverStoreOpCodec;

  @Mock
  private LifeCycleMessageCodec lifeCycleMessageCodec;

  @Mock
  private StateRepositoryOpCodec stateRepositoryOpCodec;

  @Mock
  private PassiveReplicationMessageCodec passiveReplicationMessageCodec;

  private EhcacheCodec codec;

  @Before
  public void setUp() {
    initMocks(this);

    codec = new EhcacheCodec(serverStoreOpCodec, lifeCycleMessageCodec, stateRepositoryOpCodec, null, passiveReplicationMessageCodec);
  }

  @Test
  public void encodeMessage() throws Exception {
    LifecycleMessage.DestroyServerStore lifecycleMessage = new LifecycleMessage.DestroyServerStore("foo", CLIENT_ID);
    codec.encodeMessage(lifecycleMessage);
    verify(lifeCycleMessageCodec, only()).encode(any(LifecycleMessage.class));
    verify(serverStoreOpCodec, never()).encode(any(ServerStoreOpMessage.class));
    verify(stateRepositoryOpCodec, never()).encode(any(StateRepositoryOpMessage.class));
    verify(passiveReplicationMessageCodec, never()).encode(any(PassiveReplicationMessage.class));

    ServerStoreOpMessage.ClearMessage serverStoreOpMessage = new ServerStoreOpMessage.ClearMessage("foo", CLIENT_ID);
    codec.encodeMessage(serverStoreOpMessage);
    verify(lifeCycleMessageCodec, only()).encode(any(LifecycleMessage.class));
    verify(serverStoreOpCodec, only()).encode(any(ServerStoreOpMessage.class));
    verify(stateRepositoryOpCodec, never()).encode(any(StateRepositoryOpMessage.class));
    verify(passiveReplicationMessageCodec, never()).encode(any(PassiveReplicationMessage.class));

    StateRepositoryOpMessage.EntrySetMessage stateRepositoryOpMessage = new StateRepositoryOpMessage.EntrySetMessage("foo", "bar", CLIENT_ID);
    codec.encodeMessage(stateRepositoryOpMessage);
    verify(lifeCycleMessageCodec, only()).encode(any(LifecycleMessage.class));
    verify(serverStoreOpCodec, only()).encode(any(ServerStoreOpMessage.class));
    verify(stateRepositoryOpCodec, only()).encode(any(StateRepositoryOpMessage.class));
    verify(passiveReplicationMessageCodec, never()).encode(any(PassiveReplicationMessage.class));

    ClientIDTrackerMessage clientIDTrackerMessage = new ClientIDTrackerMessage(20L, CLIENT_ID);
    codec.encodeMessage(clientIDTrackerMessage);
    verify(lifeCycleMessageCodec, only()).encode(any(LifecycleMessage.class));
    verify(serverStoreOpCodec, only()).encode(any(ServerStoreOpMessage.class));
    verify(stateRepositoryOpCodec, only()).encode(any(StateRepositoryOpMessage.class));
    verify(passiveReplicationMessageCodec, only()).encode(any(PassiveReplicationMessage.class));

  }

  @Test
  public void decodeLifeCycleMessages() throws Exception {
    for (EhcacheMessageType messageType : EhcacheMessageType.LIFECYCLE_MESSAGES) {
      ByteBuffer encodedBuffer = EhcacheCodec.OP_CODE_DECODER.encoder().enm("opCode", messageType).encode();
      codec.decodeMessage(encodedBuffer.array());
    }
    verify(lifeCycleMessageCodec, times(EhcacheMessageType.LIFECYCLE_MESSAGES.size())).decode(any(EhcacheMessageType.class), any(ByteBuffer.class));
    verifyZeroInteractions(serverStoreOpCodec, stateRepositoryOpCodec, passiveReplicationMessageCodec);
  }

  @Test
  public void decodeServerStoreMessages() throws Exception {
    for (EhcacheMessageType messageType : EhcacheMessageType.STORE_OPERATION_MESSAGES) {
      ByteBuffer encodedBuffer = EhcacheCodec.OP_CODE_DECODER.encoder().enm("opCode", messageType).encode();
      codec.decodeMessage(encodedBuffer.array());
    }
    verify(serverStoreOpCodec, times(EhcacheMessageType.STORE_OPERATION_MESSAGES.size())).decode(any(EhcacheMessageType.class), any(ByteBuffer.class));
    verifyZeroInteractions(lifeCycleMessageCodec, stateRepositoryOpCodec, passiveReplicationMessageCodec);
  }

  @Test
  public void decodeStateRepoMessages() throws Exception {
    for (EhcacheMessageType messageType : EhcacheMessageType.STATE_REPO_OPERATION_MESSAGES) {
      ByteBuffer encodedBuffer = EhcacheCodec.OP_CODE_DECODER.encoder().enm("opCode", messageType).encode();
      codec.decodeMessage(encodedBuffer.array());
    }
    verify(stateRepositoryOpCodec, times(EhcacheMessageType.STATE_REPO_OPERATION_MESSAGES.size())).decode(any(EhcacheMessageType.class), any(ByteBuffer.class));
    verifyZeroInteractions(lifeCycleMessageCodec, serverStoreOpCodec, passiveReplicationMessageCodec);
  }

  @Test
  public void decodeClientIDTrackerMessages() throws Exception {
    for (EhcacheMessageType messageType : EhcacheMessageType.PASSIVE_SYNC_MESSAGES) {
      ByteBuffer encodedBuffer = EhcacheCodec.OP_CODE_DECODER.encoder().enm("opCode", messageType).encode();
      codec.decodeMessage(encodedBuffer.array());
    }
    verify(passiveReplicationMessageCodec, times(EhcacheMessageType.PASSIVE_SYNC_MESSAGES.size())).decode(any(EhcacheMessageType.class), any(ByteBuffer.class));
    verifyZeroInteractions(lifeCycleMessageCodec, serverStoreOpCodec, stateRepositoryOpCodec);
  }
}
