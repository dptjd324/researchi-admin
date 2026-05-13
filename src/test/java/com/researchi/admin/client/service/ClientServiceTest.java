package com.researchi.admin.client.service;

import com.researchi.admin.client.domain.AdminClient;
import com.researchi.admin.client.domain.AdminClientContact;
import com.researchi.admin.client.mapper.AdminClientContactMapper;
import com.researchi.admin.client.mapper.AdminClientMapper;
import com.researchi.admin.client.web.ClientForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private AdminClientMapper adminClientMapper;
    @Mock
    private AdminClientContactMapper adminClientContactMapper;

    @InjectMocks
    private ClientService clientService;

    @Test
    void saveCreatesSinglePrimaryContact() {
        ClientForm form = new ClientForm();
        form.setClientName("Client A");
        form.setPrimaryContactName("Owner");
        form.setPrimaryEmail("owner@example.com");

        doAnswer(invocation -> {
            AdminClient client = invocation.getArgument(0);
            client.setId(9L);
            return null;
        }).when(adminClientMapper).insert(any(AdminClient.class));

        Long clientId = clientService.save(form);

        assertThat(clientId).isEqualTo(9L);
        ArgumentCaptor<AdminClientContact> contactCaptor = ArgumentCaptor.forClass(AdminClientContact.class);
        verify(adminClientContactMapper).deleteByClientId(9L);
        verify(adminClientContactMapper).insert(contactCaptor.capture());
        assertThat(contactCaptor.getAllValues()).extracting(AdminClientContact::getEmail)
                .containsExactly("owner@example.com");
        assertThat(contactCaptor.getAllValues().get(0).getPrimaryYn()).isEqualTo("Y");
    }

    @Test
    void getClientSummaryReturnsPrimaryAndActiveEmails() {
        AdminClient client = new AdminClient();
        client.setId(5L);
        client.setClientName("Client A");
        when(adminClientMapper.findById(5L)).thenReturn(client);

        AdminClientContact primary = new AdminClientContact();
        primary.setId(1L);
        primary.setClientId(5L);
        primary.setEmail("owner@example.com");
        primary.setPrimaryYn("Y");
        primary.setActiveYn("Y");

        AdminClientContact secondary = new AdminClientContact();
        secondary.setId(2L);
        secondary.setClientId(5L);
        secondary.setEmail("team@example.com");
        secondary.setPrimaryYn("N");
        secondary.setActiveYn("Y");

        when(adminClientContactMapper.findByClientId(5L)).thenReturn(List.of(secondary, primary));

        var summary = clientService.getClientSummary(5L);

        assertThat(summary.clientName()).isEqualTo("Client A");
        assertThat(summary.primaryEmail()).isEqualTo("owner@example.com");
        assertThat(summary.activeEmails()).containsExactly("owner@example.com");
    }

    @Test
    void saveUpdatesExistingClientToInactive() {
        AdminClient existing = new AdminClient();
        existing.setId(5L);
        existing.setClientName("Client A");
        existing.setActiveYn("Y");
        when(adminClientMapper.findById(5L)).thenReturn(existing);
        when(adminClientMapper.update(any(AdminClient.class))).thenReturn(1);

        ClientForm form = new ClientForm();
        form.setId(5L);
        form.setClientName("Client A");
        form.setPrimaryEmail("owner@example.com");
        form.setActive(Boolean.FALSE);

        Long clientId = clientService.save(form);

        assertThat(clientId).isEqualTo(5L);
        ArgumentCaptor<AdminClient> clientCaptor = ArgumentCaptor.forClass(AdminClient.class);
        verify(adminClientMapper).update(clientCaptor.capture());
        assertThat(clientCaptor.getValue().getActiveYn()).isEqualTo("N");
    }

    @Test
    void saveThrowsWhenExistingClientWasNotUpdated() {
        AdminClient existing = new AdminClient();
        existing.setId(5L);
        existing.setClientName("Client A");
        when(adminClientMapper.findById(5L)).thenReturn(existing);
        when(adminClientMapper.update(any(AdminClient.class))).thenReturn(0);

        ClientForm form = new ClientForm();
        form.setId(5L);
        form.setClientName("Client A");
        form.setPrimaryEmail("owner@example.com");
        form.setActive(Boolean.FALSE);

        assertThatThrownBy(() -> clientService.save(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("변경되지 않았습니다");
    }
}
