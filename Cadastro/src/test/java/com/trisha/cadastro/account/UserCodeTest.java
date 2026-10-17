package com.trisha.cadastro.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserCodeTest {

    @Test
    void of_deveFormatarNomeEmMinusculo() {
        String codigo = UserCode.of("Rafael", 1L);
        assertThat(codigo).startsWith("rafael#");
    }

    @Test
    void of_deveRemoverEspacosDoNome() {
        String codigo = UserCode.of("Rafael Silva", 2L);
        assertThat(codigo).startsWith("rafaelsilva#");
    }

    @Test
    void of_deveIncluirSequenciaNoFinal() {
        String codigo = UserCode.of("Rafael", 42L);
        assertThat(codigo).isEqualTo("rafael#42");
    }

    @Test
    void of_deveGerarCodigosDistintosParaSequenciasDiferentes() {
        String codigo1 = UserCode.of("Rafael", 1L);
        String codigo2 = UserCode.of("Rafael", 2L);
        assertThat(codigo1).isNotEqualTo(codigo2);
    }
}
