package com.trail.Cadastro.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateUtilTest {

    @Test
    void makeCode_deveFormatarNomeEmMinusculo() {
        String codigo = GenerateUtil.makeCode("Rafael", 1L);
        assertThat(codigo).startsWith("rafael#");
    }

    @Test
    void makeCode_deveRemoverEspacosDoNome() {
        String codigo = GenerateUtil.makeCode("Rafael Silva", 2L);
        assertThat(codigo).startsWith("rafaelsilva#");
    }

    @Test
    void makeCode_deveIncluirSequenciaNoFinal() {
        String codigo = GenerateUtil.makeCode("Rafael", 42L);
        assertThat(codigo).isEqualTo("rafael#42");
    }

    @Test
    void makeCode_deveGerarCodigosDistintosParaSequenciasDiferentes() {
        String codigo1 = GenerateUtil.makeCode("Rafael", 1L);
        String codigo2 = GenerateUtil.makeCode("Rafael", 2L);
        assertThat(codigo1).isNotEqualTo(codigo2);
    }
}