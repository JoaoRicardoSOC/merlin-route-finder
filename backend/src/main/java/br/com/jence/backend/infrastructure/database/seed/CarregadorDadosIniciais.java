package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.BlocoMapa;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.ProdutoAtributoJpaRepository;
import br.com.jence.backend.infrastructure.database.schema.RestricaoDeEnumNoBanco;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/**
 * Popula o banco com uma loja Leroy Merlin em miniatura para desenvolvimento e demonstracao.
 * <p>
 * As coordenadas seguem aproximadamente a planta real compartilhada pela Leroy no kickoff,
 * num grid 0-100 (x da esquerda para a direita, y de cima para baixo). Isso importa para a
 * demonstracao: e sobre esse grid que o mapa e desenhado, e com coordenadas aleatorias os
 * produtos apareceriam em lugares que nao correspondem a loja.
 * <p>
 * <b>A carga e incremental</b>, nao tudo-ou-nada: cada secao e cada produto so e criado se
 * ainda nao existir. Assim um produto novo acrescentado aqui chega tambem aos bancos que ja
 * tinham a massa antiga - inclusive o da instancia publicada. Ver D-47.
 * <p>
 * Pode ser desligado com {@code merlin.seed.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "merlin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CarregadorDadosIniciais implements ApplicationRunner {

    /*
     * Teto da leitura que descobre o que ja existe. Precisa ser maior que o catalogo de
     * demonstracao com folga; se um dia for ultrapassado, a carga se recusa a rodar em vez de
     * arriscar inserir SKU duplicado.
     */
    private static final int LIMITE_DE_LEITURA = 1000;

    /*
     * Tipos de ponto que existiram no banco e sairam do enum. Enquanto a linha continuar la,
     * qualquer leitura que traga todos os pontos - a planta da loja, por exemplo - quebra na
     * conversao para TipoPonto. Como a carga e incremental e nunca apaga nada, este e o unico
     * lugar que pode limpar a massa de quem ja rodou a versao anterior.
     */
    private static final List<String> TIPOS_APOSENTADOS = List.of("TOTEM");

    /*
     * Corredores que trocaram de nome, do antigo para o atual.
     *
     * Para as secoes o risco e duplicar: a carga casa secao existente pelo nome do corredor,
     * entao renomear na PlantaDaLoja sem este passo criaria um ponto novo e vazio, e os
     * produtos ficariam presos ao de nome velho - a secao apareceria duas vezes no mapa, uma
     * com tudo e outra com nada.
     *
     * Para a placa e o banheiro o risco e o oposto: eles sao casados por codigo curto e por
     * tipo, entao nunca duplicam - e por isso mesmo o texto antigo ficaria gravado para
     * sempre, e e texto que o cliente le na tela de localizacao.
     *
     * Renomear no lugar preserva o id, e a chave estrangeira dos produtos nao se move. Ver
     * D-70. Entradas cujo nome antigo nao existe mais em banco nenhum podem sair daqui.
     */
    private static final Map<String, String> CORREDORES_RENOMEADOS = Map.of(
            "Eletrica", "Elétrica",
            "Iluminacao", "Iluminação",
            "Decoracao", "Decoração",
            "Materiais de construcao", "Materiais de construção",
            "Sanitarios", "Sanitários",
            "Corredor leste, junto a Iluminacao", "Corredor leste, junto a Iluminação");

    /*
     * URLs publicas das fotos, coletadas do site da Leroy pelo time (O-18). Enquanto um SKU
     * nao estiver aqui, o produto responde com imagem nula - o que a tela precisa tratar, e
     * nao um estado invalido. Acrescentar uma URL aqui chega aos bancos que ja existem pelo
     * passo de sincronizarApresentacoes.
     *
     * O site publica varias fotos por produto; aqui fica a primeira, porque o produto guarda
     * uma imagem so. A lista completa do que foi visitado esta em docs/imagens-dos-produtos.md.
     */
    private static final Map<String, String> IMAGENS = Map.ofEntries(
            entry("SKU-COZ-001",
                    "https://cdn.leroymerlin.com.br/products/cuba_retangular_tramontina_em_aco_inox_acetinado_56x34cm_56_b_1566754993_5d96_600x600.jpg"),
            entry("SKU-COZ-002",
                    "https://cdn.leroymerlin.com.br/products/torneira_monocomando_de_pia_bica_alta_cromado_tomas_delinia_92308153_057c_600x600.png"),
            entry("SKU-COZ-003",
                    "https://cdn.leroymerlin.com.br/products/cuba_de_embutir_retangular_40_bl_standard_40x34_cm_sem_valvul_1571090792_985a_600x600.jpg"),
            entry("SKU-COZ-004",
                    "https://cdn.leroymerlin.com.br/products/cuba_para_cozinha_dupla_de_embutir_40x17x70cm_escovado_90834422_b6be_600x600.jpg"),
            entry("SKU-COZ-005",
                    "https://cdn.leroymerlin.com.br/products/torneira_misturador_de_parede_bica_alta_cromada_sao_delinia_92420762_4610_600x600.jpg"),
            entry("SKU-COZ-006",
                    "https://cdn.leroymerlin.com.br/products/torneira_monocomando_de_pia_bica_alta_preto_econocozi_jiwi_92323371_e005_600x600.JPG"),
            entry("SKU-COZ-007",
                    "https://cdn.leroymerlin.com.br/products/lixeira_inox_escovado_5_litros_embutir_pia_cozinha_cesto_lixo_1572480011_ea6a_600x600.png"),
            entry("SKU-COZ-008",
                    "https://cdn.leroymerlin.com.br/products/escorredor_de_loucas_de_embutir_bandeja_inox_77x26cm_schmitt_1571389776_e170_600x600.png"),
            entry("SKU-COZ-009",
                    "https://cdn.leroymerlin.com.br/products/puxador_de_movel_aluminio_preto_128mm_java_92315902_56bf_600x600.jpg"),
            entry("SKU-COZ-010",
                    "https://cdn.leroymerlin.com.br/products/rejunte_epoxi_quartzolit_cores_1kg_ceramica_porcelanato_branco_1567293979_78db_600x600.png"),
            entry("SKU-DEC-001",
                    "https://cdn.leroymerlin.com.br/products/espelho_redondo_led_bivolt_60cm_com_led_gavix_92462440_dcc4_600x600.jpg"),
            entry("SKU-DEC-002",
                    "https://cdn.leroymerlin.com.br/products/_92358392_fa7a_600x600.jpg"),
            entry("SKU-DEC-003",
                    "https://cdn.leroymerlin.com.br/products/espelho_decorativo_retangular_80x60cm_corino_lumina_92527960_0cb1_600x600.jpg"),
            entry("SKU-DEC-004",
                    "https://cdn.leroymerlin.com.br/products/quadro_arte_manual_dourado_40x60cm_arte_propria_92052016_a21b_600x600.jpg"),
            entry("SKU-DEC-005",
                    "https://cdn.leroymerlin.com.br/products/prateleira_suspensa_60cm_parede_nicho_de_madeira___suporte_1572341279_3c70_600x600.jpg"),
            entry("SKU-DEC-006",
                    "https://cdn.leroymerlin.com.br/products/cortina_alycia_2,60x1,80m_moon_inspire_91903350_0001_600x600.jpg"),
            entry("SKU-DEC-007",
                    "https://cdn.leroymerlin.com.br/products/tapete_de_banheiro_em_microfibra_retangular_bege_1_peca_oikos_92425396_7e77_600x600.jpg"),
            entry("SKU-DEC-008",
                    "https://cdn.leroymerlin.com.br/products/papel_de_parede_autocolante_azulejo_ladrilho_marmore_calacatt_1570857265_fbf5_600x600.jpg"),
            entry("SKU-DEC-009",
                    "https://cdn.leroymerlin.com.br/products/cabideiro_de_parede_com_5_ganchos_para_pendurar_roupas_e_bols_1570051837_06ed_600x600.jpg"),
            entry("SKU-DEC-010",
                    "https://cdn.leroymerlin.com.br/products/vaso_decorativo_vidro_tubo_transparente_25cm_unico_1571745978_e474_600x600.jpg"),
            entry("SKU-ELE-001",
                    "https://cdn.leroymerlin.com.br/products/cabo_flexivel_azul_2_50_rolo_100m_87807090_0002_600x600.jpg"),
            entry("SKU-ELE-002",
                    "https://cdn.leroymerlin.com.br/products/interruptor_simples_4x2_c__1_tecla_10a_250v_branco_tramontina_1568977079_2a81_600x600.jpg"),
            entry("SKU-ELE-003",
                    "https://cdn.leroymerlin.com.br/products/disjuntor_bipolar_din_curva_c_25a_steck_88507636_0001_600x600.jpg"),
            entry("SKU-ELE-004",
                    "https://cdn.leroymerlin.com.br/products/cabo_flexivel_1,5mm_100m_amarelo_750v_cobrecom_91953736_8bb5_600x600.jpg"),
            entry("SKU-ELE-005",
                    "https://cdn.leroymerlin.com.br/products/cabo_flexivel_4mm_50m_preto_750v_cobrecom_91923300_db0b_600x600.jpg"),
            entry("SKU-ELE-006",
                    "https://cdn.leroymerlin.com.br/products/interruptor_simples_4x2_c__1_tecla_paralelo_10a_250v_branco_t_1568977080_2829_600x600.jpg"),
            entry("SKU-ELE-007",
                    "https://cdn.leroymerlin.com.br/products/tomada_2p_t_10a_250v_evidence_fame_1568949602_2a24_600x600.png"),
            entry("SKU-ELE-008",
                    "https://cdn.leroymerlin.com.br/products/tomada_dupla_fame_habitat_2p_t_20a_com_placa_4x2_branco_1570752212_5570_600x600.jpg"),
            entry("SKU-ELE-009",
                    "https://cdn.leroymerlin.com.br/products/disjuntor_mini_din_unipolar_16a_curva_b_127_220v_siemens_1568442761_f091_600x600.jpg"),
            entry("SKU-ELE-010",
                    "https://cdn.leroymerlin.com.br/products/disjuntor_din_tripolar_220_400v_40a_eletromar_87912741_d136_600x600.jpg"),
            entry("SKU-ELE-011",
                    "https://cdn.leroymerlin.com.br/products/quadro_pvc_embutir_12_disjuntores_branco_1570461144_e57a_600x600.jpg"),
            entry("SKU-ENC-001",
                    "https://cdn.leroymerlin.com.br/products/cano_marrom_pvc_25mm_ou_3_4_3m_tigre_85949885_47a1_600x600.jpg"),
            entry("SKU-ENC-002",
                    "https://cdn.leroymerlin.com.br/products/cola_para_pvc_incolor_frasco_175g_tigre_87846101_c4bf_600x600.jpeg"),
            entry("SKU-ENC-003",
                    "https://cdn.leroymerlin.com.br/products/torneira_de_lavatorio_com_bica_fixa_baixa_prata_remix_sensea_92335852_853d_600x600.png"),
            entry("SKU-ENC-004",
                    "https://cdn.leroymerlin.com.br/products/tubo_extensivo_universal_plastico_branco_tigre_88126794_99e6_600x600.jpeg"),
            entry("SKU-ENC-005",
                    "https://cdn.leroymerlin.com.br/products/sifao_extensivel_universal_com_copo__blukit_cromado__1567493456_9c43_600x600.jpg"),
            entry("SKU-ENC-006",
                    "https://cdn.leroymerlin.com.br/products/cano_soldavel_1m_32mm_1_marrom_equation_90119876_a3fc_600x600.jpg"),
            entry("SKU-ENC-007",
                    "https://cdn.leroymerlin.com.br/products/joelho_90o_solda_marrom_25mm_3_4_tigre_85307075_0001_600x600.jpg"),
            entry("SKU-ENC-008",
                    "https://cdn.leroymerlin.com.br/products/te_90o_solda_marrom_25mm_3_4_tigre_85306984_0001_600x600.jpg"),
            entry("SKU-ENC-009",
                    "https://cdn.leroymerlin.com.br/products/registro_de_gaveta_bruto_25mm_ou_1_deca_90026223_0001_600x600.jpg"),
            entry("SKU-ENC-010",
                    "https://cdn.leroymerlin.com.br/products/valvula_pia_cozinha_4_1_2_aco_inox_com_cesto_higienico_removi_1572513121_75ea_600x600.png"),
            entry("SKU-ENC-011",
                    "https://cdn.leroymerlin.com.br/products/fita_veda_rosca_18mm_x_50m_equation_91875056_99b2_600x600.jpg"),
            entry("SKU-ENC-012",
                    "https://cdn.leroymerlin.com.br/products/caixa_sifonada_quadrada_com_3_entradas_branca_100x100x50mm___1567564327_692c_600x600.jpg"),
            entry("SKU-FER-001",
                    "https://cdn.leroymerlin.com.br/products/furadeira_de_impacto_1_2_650w_127v__110v__dexter_90760964_43c4_600x600.jpeg"),
            entry("SKU-FER-002",
                    "https://cdn.leroymerlin.com.br/products/trena_profissional_irwin_5m_16ft_x_3_4_87421054_0001.jpg_600x600.jpg"),
            entry("SKU-FER-003",
                    "https://cdn.leroymerlin.com.br/products/trena_standard_amarela_stein_7,5m_25mm_1570271787_bce4_600x600.jpg"),
            entry("SKU-FER-004",
                    "https://cdn.leroymerlin.com.br/products/furadeira_de_impacto_850w_110v_gsb_16_re_bosch_91988085_cb69_600x600.jpg"),
            entry("SKU-FER-005",
                    "https://cdn.leroymerlin.com.br/products/parafusadeira_ranger_a_bateria_12v_3_8_bivolt_92446263_0b6b_600x600.jpg"),
            entry("SKU-FER-006",
                    "https://cdn.leroymerlin.com.br/products/jogo_de_broca_para_concreto_4mm_a_10mm_5_pecas_dexter_91950236_58cd_600x600.jpg"),
            entry("SKU-FER-007",
                    "https://cdn.leroymerlin.com.br/products/jogo_chave_fenda_e_philips_cromo_vanadio_c__6_pecas_vonder_1566849821_531e_600x600.jpg"),
            entry("SKU-FER-008",
                    "https://cdn.leroymerlin.com.br/products/martelo_borracha_cabo_de_madeira_26cm_dexter_90114052_e532_600x600.jpg"),
            entry("SKU-FER-009",
                    "https://cdn.leroymerlin.com.br/products/alicate_universal_eletricista_8_polegadas_corneta_4235203_1567397237_6fc2_600x600.jpg"),
            entry("SKU-FER-010",
                    "https://cdn.leroymerlin.com.br/products/nivel_manual_de_aluminio_16__400mm__2_bolhas_dexter_91082005_6765_600x600.JPG"),
            entry("SKU-FER-011",
                    "https://cdn.leroymerlin.com.br/products/serrote_profissional_tamanho_20_cabo_em_madeira_lamina_em_aco_1567821334_9aff_600x600.jpg"),
            entry("SKU-FER-012",
                    "https://cdn.leroymerlin.com.br/products/escada_aluminio_5_degraus_1,53m_120kg_prata_e_vermelho_91713286_0001_600x600.jpg"),
            entry("SKU-FRG-001",
                    "https://cdn.leroymerlin.com.br/products/parafuso_chipboard_cabeca_chata_4x40mm_20_pecas_standers_92061592_5952_600x600.jpg"),
            entry("SKU-FRG-002",
                    "https://cdn.leroymerlin.com.br/products/parafuso_em_aco_4,8x50mm_cabeca_chata_soberba_15_unidades_92028923_45fb_600x600.jpg"),
            entry("SKU-FRG-003",
                    "https://cdn.leroymerlin.com.br/products/parafuso_para_madeira_3,5x30mm_com_30_pecas_92117326_ed08_600x600.jpg"),
            entry("SKU-FRG-004",
                    "https://cdn.leroymerlin.com.br/products/parafuso_chipboard_cabeca_flangeada_phillips_5x30mm_caixa_com_1571420082_4cc8_600x600.jpg"),
            entry("SKU-FRG-005",
                    "https://cdn.leroymerlin.com.br/products/kit_cabo_de_aco_varal_16mm_plastificado_15_metros_com_4_ganc_1572843845_1dd2_600x600.jpg"),
            entry("SKU-FRG-006",
                    "https://cdn.leroymerlin.com.br/products/parafuso_em_aco_6,1x65mm_cabeca_chata_soberba_10_unidades_92029000_862e_600x600.jpg"),
            entry("SKU-FRG-007",
                    "https://cdn.leroymerlin.com.br/products/dobradica_aco_vai_e_vem_bang_bang_retorno_mola_automatico_3_p_1570869662_92c4_600x600.png"),
            entry("SKU-FRG-008",
                    "https://cdn.leroymerlin.com.br/products/fechadura_pado_para_porta_interna_cromado_40mm_concept_92101093_24e6_600x600.JPG"),
            entry("SKU-FRG-009",
                    "https://cdn.leroymerlin.com.br/products/cadeado_com_chave_simples_40mm_latao_aco_cromado_pado_89999203_0001_600x600.jpg"),
            entry("SKU-FRG-010",
                    "https://cdn.leroymerlin.com.br/products/mao_francesa_normo_30cm_branca_92034950_82f2_600x600.jpg"),
            entry("SKU-FRG-011",
                    "https://cdn.leroymerlin.com.br/products/arruela_plana_grande_8mm_aco_standers_10_pecas_92062285_7666_600x600.png"),
            entry("SKU-ILU-001",
                    "https://cdn.leroymerlin.com.br/products/lampada_led_luz_branca_9w_bivolt_89792101_99e9_600x600.jpeg"),
            entry("SKU-ILU-002",
                    "https://cdn.leroymerlin.com.br/products/luminaria_bivolt_embutir_led_slim_quadrada_startec_preto_1570559659_7d10_600x600.jpg"),
            entry("SKU-ILU-003",
                    "https://cdn.leroymerlin.com.br/products/kit_3_lampada_led_bulbo_12w_branco_quente__3000k__ourolux_1569958927_b30c_600x600.png"),
            entry("SKU-ILU-004",
                    "https://cdn.leroymerlin.com.br/products/kit_10_lampadas_led_kian_classic_9w_bivolt_3000k_amarela_1572874551_789a_600x600.jpg"),
            entry("SKU-ILU-005",
                    "https://cdn.leroymerlin.com.br/products/kit_com_10_lampadas_led_bulbo_luz_branca_a60_15w_bivolt_elgin_91963956_6afc_600x600.JPG"),
            entry("SKU-ILU-006",
                    "https://cdn.leroymerlin.com.br/products/lampada_led_de_filamento_luz_ambar_4w_bivolt_90537895_2c2a_600x600.jpg"),
            entry("SKU-ILU-007",
                    "https://cdn.leroymerlin.com.br/products/luminaria_led_embutir_redonda_18w_1569920103_7d40_600x600.jpg"),
            entry("SKU-ILU-008",
                    "https://cdn.leroymerlin.com.br/products/painel_de_led_sobrepor_luz_branca_24w_30x30cm_92409016_78a1_600x600.jpg"),
            entry("SKU-ILU-009",
                    "https://cdn.leroymerlin.com.br/products/trilho_eletrificado_2m___6_spot_led_7w_6000k_bivolt_preto_1567610452_6a00_600x600.jpg"),
            entry("SKU-ILU-010",
                    "https://cdn.leroymerlin.com.br/products/fita_led_rolo_5m_branco_frio_3528_dupla_face_ip65_com_fonte_1569606082_70a2_600x600.jpg"),
            entry("SKU-ILU-011",
                    "https://cdn.leroymerlin.com.br/products/arandela_externa_solar_com_sensor_movimento_300lm_inspire_91072863_3585_600x600.jpg"),
            entry("SKU-JAR-001",
                    "https://cdn.leroymerlin.com.br/products/vaso_de_ceramica_decorativo_para_flores_laranja_terra_g_30_cm_1571462729_98a1_600x600.jpg"),
            entry("SKU-JAR-002",
                    "https://cdn.leroymerlin.com.br/products/terra_jardim__geolia_20kg_87319043_0001_600x600.jpg"),
            entry("SKU-JAR-003",
                    "https://cdn.leroymerlin.com.br/products/vaso_de_planta_pequeno_9x15,2cm_orquidea_92174425_9d55_600x600.png"),
            entry("SKU-JAR-004",
                    "https://cdn.leroymerlin.com.br/products/vaso_planta_65x40_oval_moderno_polietileno_cinza_cimento_004_1571446564_0a77_600x600.jpg"),
            entry("SKU-JAR-005",
                    "https://cdn.leroymerlin.com.br/products/substrato_para_folhagem_solido_organico_25l_92378454_3e73_600x600.JPG"),
            entry("SKU-JAR-006",
                    "https://cdn.leroymerlin.com.br/products/fertilizante_premium_npk_10_10_10_uso_geral_cpc_garden_1kg_1571812801_90b3_600x600.jpg"),
            entry("SKU-JAR-007",
                    "https://cdn.leroymerlin.com.br/products/mangueira_geoconfort_20m_preto_90801193_0001_600x600.jpg"),
            entry("SKU-JAR-008",
                    "https://cdn.leroymerlin.com.br/products/regador_plastico_4,5l_preto_famastil_91832776_e378_600x600.jpg"),
            entry("SKU-JAR-009",
                    "https://cdn.leroymerlin.com.br/products/alicate_tesoura_para_poda_manual_com_trava_e_mola_8_polegadas_1571928233_5580_600x600.jpg"),
            entry("SKU-JAR-010",
                    "https://cdn.leroymerlin.com.br/products/pedra_dolomita_branca_pequena_10kg_87441921_0001_600x600.jpg"),
            entry("SKU-JAR-011",
                    "https://cdn.leroymerlin.com.br/products/grama_sintetica_softgrass_10mm_2x1m_2m2_decortech_1566949335_f485_600x600.jpg"),
            entry("SKU-MAT-001",
                    "https://cdn.leroymerlin.com.br/products/argamassa_acii_interno_e_externo_cinza_20kg_89339621_68fb_600x600.jpeg"),
            entry("SKU-MAT-002",
                    "https://cdn.leroymerlin.com.br/products/cimento_todas_as_obras_50kg_89368566_31e9_600x600.jpg"),
            entry("SKU-MAT-003",
                    "https://cdn.leroymerlin.com.br/products/argamassa_colante_multiuso_aciii__20kg_branca_axton_89388684_a5be_600x600.jpg"),
            entry("SKU-MAT-004",
                    "https://cdn.leroymerlin.com.br/products/cimento_cp_ii_branco_1kg_fortaleza_87519124_355b_600x600.jpg"),
            entry("SKU-MAT-005",
                    "https://cdn.leroymerlin.com.br/products/cal_hidratada_para_construcao_civil_20kg_votoran_85981945_1b57_600x600.JPG"),
            entry("SKU-MAT-006",
                    "https://cdn.leroymerlin.com.br/products/areia_fina_saco_20kg_tres_lagoas_89953276_0001_600x600.jpg"),
            entry("SKU-MAT-007",
                    "https://cdn.leroymerlin.com.br/products/bloco_ceramico_8_furos_9x19x19cm_ceramica_volpini_89270272_f63c_600x600.jpg"),
            entry("SKU-MAT-008",
                    "https://cdn.leroymerlin.com.br/products/tijolo_refratario_11,4x5,1x22,9cm_gabriella_92321334_9d8c_600x600.jpg"),
            entry("SKU-MAT-009",
                    "https://cdn.leroymerlin.com.br/products/rejunte_acrilico_rejunte_base_plastica_cinza_1kg_axton_92108534_e195_600x600.jpg"),
            entry("SKU-MAT-010",
                    "https://cdn.leroymerlin.com.br/products/manta_liquida_vedapren_preta_18l_vedacit_89109965_a173_600x600.jpg"),
            entry("SKU-MAT-011",
                    "https://cdn.leroymerlin.com.br/products/tela_soldada_tag_malha_5x10cm_fio_1,60mm_rl_25x1,0m_1571551105_e5b3_600x600.jpg"),
            entry("SKU-TIN-001",
                    "https://cdn.leroymerlin.com.br/products/tinta_acrilica_eggshell_super_lavavel_interior_gelo_18_l_89594302_db0d_600x600.jpg"),
            entry("SKU-TIN-002",
                    "https://cdn.leroymerlin.com.br/products/rolo_la_de_carneiro_9cm_1379_tigre_87098851_8549_600x600.jpg"),
            entry("SKU-TIN-003",
                    "https://cdn.leroymerlin.com.br/products/jogo_lixa_para_maquina_roto_orbital_parede_grao_120_225mm_wbr_90067320_af46_600x600.jpg"),
            entry("SKU-TIN-004",
                    "https://cdn.leroymerlin.com.br/products/lixa_para_metal_dagua_grao_150_225x275cm_dexter_92152060_9857_600x600.jpg"),
            entry("SKU-TIN-005",
                    "https://cdn.leroymerlin.com.br/products/fita_crepe_48mm_x_50m_tigre_1572256340_581c_600x600.jpg"),
            entry("SKU-TIN-006",
                    "https://cdn.leroymerlin.com.br/products/tinta_acrilica_fosca_klasse_economica_interior_branca_3,6l_92302252_8655_600x600.jpg"),
            entry("SKU-TIN-007",
                    "https://cdn.leroymerlin.com.br/products/tinta_semi_acetinada_super_lavavel_premium_interno_branca_20l_92311660_1727_600x600.jpg"),
            entry("SKU-TIN-008",
                    "https://cdn.leroymerlin.com.br/products/esmalte_sintetico_standard_maza_branco_brilhante_900ml_1572809047_989e_600x600.jpg"),
            entry("SKU-TIN-009",
                    "https://cdn.leroymerlin.com.br/products/massa_pva_corrida_eucatex_balde_25kg_1572096855_4352_600x600.jpg"),
            entry("SKU-TIN-010",
                    "https://cdn.leroymerlin.com.br/products/selador_acrilico_pre_pintura_18l_suvinil_92394141_d4d2_600x600.jpg"),
            entry("SKU-TIN-011",
                    "https://cdn.leroymerlin.com.br/products/bandeja_plastica_preta_23cm_87565982_0ab8_600x600.png"),
            entry("SKU-TIN-012",
                    "https://cdn.leroymerlin.com.br/products/pincel_chato_tigre_815_2_embalagem_com_12_unidades_1568055444_ac54_600x600.jpg"));

    /** Nome, descricao e imagem de um produto, como a massa os declara hoje. */
    private record Apresentacao(String nome, String descricao, String imagemUrl) {
    }

    private final ProdutoRepository produtoRepository;
    private final PontoMapaRepository pontoMapaRepository;
    private final PontoMapaJpaRepository pontoMapaJpaRepository;
    private final ProdutoAtributoJpaRepository atributoJpaRepository;
    private final RestricaoDeEnumNoBanco restricaoDeEnum;

    /*
     * O que esta execucao criou. Vive numa instancia propria por chamada, e nao em campo do
     * componente: como ele e um singleton do Spring, contadores de instancia se somariam entre
     * execucoes e o log passaria a mentir a partir da segunda - foi o que um teste flagrou.
     */
    private static final class Contagem {
        private int pontos;
        private int renomeados;
        private int produtos;
        private int apresentacoes;
        private int atributos;
        private int estoques;

        private boolean nadaFeito() {
            return pontos == 0 && renomeados == 0 && produtos == 0
                    && apresentacoes == 0 && atributos == 0 && estoques == 0;
        }
    }

    /*
     * Montado enquanto o catalogo e declarado, e consumido logo depois para completar o que ja
     * estava gravado. Vive numa instancia por execucao pelo mesmo motivo da Contagem: o
     * componente e um singleton do Spring.
     */
    private final Map<String, Apresentacao> apresentacoes = new LinkedHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        Pagina<Produto> existentes = produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA);

        if (existentes.totalElementos() > existentes.conteudo().size()) {
            log.warn("Catalogo tem {} produtos, acima do limite de leitura da carga inicial ({}). "
                            + "Carga ignorada para nao arriscar duplicar SKU.",
                    existentes.totalElementos(), LIMITE_DE_LEITURA);
            return;
        }

        Set<String> skusExistentes = existentes.conteudo().stream()
                .map(Produto::getSku)
                .collect(Collectors.toSet());

        Contagem contagem = new Contagem();
        /*
          * A ordem importa: apagar as linhas de tipo aposentado antes de refazer a restricao,
          * porque uma linha com valor fora do enum faria o "add check" ser recusado.
          */
        apagarPontosDeTipoAposentado();
        renomearCorredores(contagem);
        restricaoDeEnum.sincronizar();

        Map<String, PontoMapa> secoes = carregarOuCriarSecoes(contagem);
        criarPontosDeServicoQueFaltam(contagem);
        criarPontosDeQrCodeQueFaltam(contagem);
        apresentacoes.clear();
        criarCatalogo(secoes, skusExistentes, contagem);
        sincronizarApresentacoes(contagem);
        sincronizarAtributos(contagem);
        sincronizarEstoque(contagem);

        if (contagem.nadaFeito()) {
            log.info("Massa de demonstracao ja esta completa. Nada a carregar.");
        } else {
            log.info("Carga incremental: {} ponto(s) do mapa, {} corredor(es) renomeado(s), "
                            + "{} produto(s) criados, {} apresentacao(oes) sincronizada(s), "
                            + "{} produto(s) com caracteristicas atualizadas e {} com estoque "
                            + "ajustado.",
                    contagem.pontos, contagem.renomeados, contagem.produtos,
                    contagem.apresentacoes, contagem.atributos, contagem.estoques);
        }
    }

    // ---------------------------------------------------------------- pontos do mapa

    private void apagarPontosDeTipoAposentado() {
        for (String tipo : TIPOS_APOSENTADOS) {
            int apagados = pontoMapaJpaRepository.apagarPorTipoBruto(tipo);
            if (apagados > 0) {
                log.info("Ponto de tipo {}, aposentado pelo escopo revisado: {} linha(s) apagada(s).",
                        tipo, apagados);
            }
        }
    }

    /*
     * Precisa rodar antes de carregarOuCriarSecoes: depois, a secao de nome novo ja teria sido
     * criada vazia e a renomeacao esbarraria na unicidade do corredor.
     */
    private void renomearCorredores(Contagem contagem) {
        CORREDORES_RENOMEADOS.forEach((antigo, novo) -> {
            /*
             * Limpa antes de renomear. Se uma execucao anterior ja criou a secao com o nome
             * novo - o que acontece quando alguem roda a planta acentuada sem esta migracao -,
             * renomear por cima deixaria duas linhas com o mesmo nome, uma com os produtos e
             * outra vazia. A vazia nao serve para nada e ninguem a referencia.
             */
            int vazias = pontoMapaJpaRepository.apagarPrateleiraVaziaChamada(novo);
            if (vazias > 0) {
                log.info("Secao '{}' existia vazia e duplicada: {} ponto(s) apagado(s) antes "
                        + "de renomear.", novo, vazias);
            }

            int linhas = pontoMapaJpaRepository.renomearCorredor(antigo, novo);
            if (linhas > 0) {
                log.info("Corredor '{}' renomeado para '{}': {} ponto(s) atualizado(s). "
                        + "Os produtos continuam no mesmo ponto.", antigo, novo, linhas);
                contagem.renomeados += linhas;
            }
        });
    }

    private Map<String, PontoMapa> carregarOuCriarSecoes(Contagem contagem) {
        Map<String, PontoMapa> existentes = pontoMapaRepository.buscarPorTipo(TipoPonto.PRATELEIRA)
                .stream()
                .collect(Collectors.toMap(PontoMapa::getCorredor, Function.identity(), (a, b) -> a));

        Map<String, PontoMapa> secoes = new LinkedHashMap<>();
        /*
         * A coordenada vem do centro do bloco, e nao de um numero digitado aqui: e o que
         * garante que um produto nunca apareca fora do proprio corredor no mapa. Acrescentar
         * uma secao comeca por acrescentar um bloco em PlantaDaLoja. Ver D-58.
         */
        for (BlocoMapa bloco : PlantaDaLoja.blocos()) {
            registrar(secoes, existentes, contagem, bloco);
        }
        return secoes;
    }

    private void registrar(Map<String, PontoMapa> secoes, Map<String, PontoMapa> existentes,
                           Contagem contagem, BlocoMapa bloco) {
        PontoMapa ponto = existentes.get(bloco.rotulo());
        if (ponto == null) {
            ponto = pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(),
                    TipoPonto.PRATELEIRA, bloco.rotulo(), bloco.centroX(), bloco.centroY()));
            contagem.pontos++;
        }
        secoes.put(bloco.rotulo(), ponto);
    }

    /* Nao vendem nada, mas precisam aparecer no mapa: o cliente vai ate os caixas para
     * fechar a compra, e o banheiro e uma parada que ele pode querer localizar. */
    private void criarPontosDeServicoQueFaltam(Contagem contagem) {
        criarSeNaoHouver(contagem, TipoPonto.CAIXA, "Frente de loja", 62, 88);
        criarSeNaoHouver(contagem, TipoPonto.BANHEIRO, "Sanitários", 52, 8);
    }

    /*
     * Onde os adesivos ficam colados: corredores de passagem e cruzamentos, nao dentro das
     * secoes - o cliente escaneia enquanto anda, nao quando ja chegou onde queria.
     *
     * O codigo impresso no adesivo leva hifen (ENT-01) porque e mais facil de ler e de ditar;
     * o banco guarda a forma canonica (ENT01) e a busca normaliza a digitacao, entao o hifen e
     * so tipografia. Ver D-52.
     *
     * Quantos e exatamente onde ainda e decisao do time (O-18): trocar as coordenadas aqui nao
     * afeta nenhuma outra parte do sistema.
     */
    private void criarPontosDeQrCodeQueFaltam(Contagem contagem) {
        criarQrCodeSeNaoHouver(contagem, "ENT-01", "Entrada da loja", 50, 92);
        criarQrCodeSeNaoHouver(contagem, "TIN-02", "Corredor de Tintas", 32, 18);
        criarQrCodeSeNaoHouver(contagem, "CEN-03", "Cruzamento central", 41, 40);
        criarQrCodeSeNaoHouver(contagem, "ILU-04", "Corredor leste, junto a Iluminação", 76, 42);
        criarQrCodeSeNaoHouver(contagem, "FER-05", "Corredor oeste, junto a Ferramentas", 20, 65);
        criarQrCodeSeNaoHouver(contagem, "CAI-06", "Frente de loja, antes dos caixas", 62, 80);
    }

    private void criarQrCodeSeNaoHouver(Contagem contagem, String codigo, String corredor,
                                        int x, int y) {
        if (pontoMapaRepository.buscarPorCodigoCurto(codigo).isEmpty()) {
            pontoMapaRepository.salvar(
                    new PontoMapa(UUID.randomUUID(), TipoPonto.QR_CODE, corredor, x, y, codigo));
            contagem.pontos++;
        }
    }

    private void criarSeNaoHouver(Contagem contagem, TipoPonto tipo, String corredor, int x, int y) {
        if (pontoMapaRepository.buscarPorTipo(tipo).isEmpty()) {
            pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), tipo, corredor, x, y));
            contagem.pontos++;
        }
    }

    // ---------------------------------------------------------------- catalogo

    /**
     * Cria os produtos que ainda nao existem, a partir de {@link CatalogoDaMassa}.
     * <p>
     * Uma fonte so para nome, preco, descricao e caracteristicas: acrescentar um produto e
     * acrescentar uma entrada la, e nao editar tres lugares em paralelo. Ver D-66.
     */
    private void criarCatalogo(Map<String, PontoMapa> secoes, Set<String> jaExistentes,
                               Contagem contagem) {
        for (ProdutoDaMassa declarado : CatalogoDaMassa.produtos()) {
            PontoMapa ponto = secoes.get(declarado.secao());
            if (ponto == null) {
                // Secao sem bloco na planta: o produto ficaria sem lugar no mapa (D-58).
                log.warn("Produto {} declara a secao '{}', que nao existe na planta. Ignorado.",
                        declarado.sku(), declarado.secao());
                continue;
            }

            apresentacoes.put(declarado.sku(), new Apresentacao(
                    declarado.nome(), declarado.descricao(), IMAGENS.get(declarado.sku())));

            if (jaExistentes.contains(declarado.sku())) {
                continue;
            }

            produtoRepository.salvar(new Produto(UUID.randomUUID(), declarado.sku(),
                    declarado.nome(), declarado.descricao(), IMAGENS.get(declarado.sku()),
                    declarado.precoEmReais(), declarado.estoque(), ponto));
            contagem.produtos++;
        }
    }

    /**
     * Faz o saldo em estoque voltar a ser o que a massa declara.
     *
     * <p>Sem este passo, mudar um estoque no codigo nao chegaria a banco nenhum que ja tenha o
     * produto - e como o schema e um so para tudo (O-21), isso significa nao chegar a lugar
     * nenhum. Foi o que aconteceu quando a lixa da ruptura passou a nascer com estoque: os
     * testes continuaram vendo a massa antiga.
     *
     * <p><b>Estoque e dado declarado aqui, e nao estado de um ERP.</b> Nao existe integracao
     * de inventario (D-23): a massa e a unica fonte. O endpoint de simulacao (D-40) escreve
     * por cima para encenar, e este passo devolve o valor ensaiado no proximo start - o que e
     * util, e nao atrapalha, porque a ruptura deixou de depender de zerar produto (D-72).
     *
     * <p>Setima vez que o mesmo padrao aparece: campo de fora da reconciliacao envelhece em
     * silencio. Ver D-51, D-53, D-56, D-59, D-69 e D-70.
     */
    private void sincronizarEstoque(Contagem contagem) {
        Map<String, Integer> declarados = CatalogoDaMassa.produtos().stream()
                .collect(Collectors.toMap(ProdutoDaMassa::sku, ProdutoDaMassa::estoque));

        for (Produto produto : produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA).conteudo()) {
            Integer esperado = declarados.get(produto.getSku());
            if (esperado == null || esperado == produto.getSaldoEstoque()) {
                continue;
            }
            log.info("Estoque de {} ajustado de {} para {}, como a massa declara.",
                    produto.getSku(), produto.getSaldoEstoque(), esperado);
            produtoRepository.salvar(produto.comSaldoEstoque(esperado));
            contagem.estoques++;
        }
    }

    private void sincronizarAtributos(Contagem contagem) {
        Map<String, List<ValorDeAtributo>> declarados = CatalogoDaMassa.produtos().stream()
                .collect(Collectors.toMap(ProdutoDaMassa::sku, ProdutoDaMassa::atributos));

        Map<UUID, List<ValorDeAtributo>> gravados = atributoJpaRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        entity -> entity.getProduto().getId(),
                        Collectors.mapping(
                                entity -> new ValorDeAtributo(entity.getChave(), entity.getValor()),
                                Collectors.toList())));

        for (Produto produto : produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA).conteudo()) {
            List<ValorDeAtributo> esperados = declarados.get(produto.getSku());
            if (esperados == null) {
                continue;
            }

            List<ValorDeAtributo> atuais = gravados.getOrDefault(produto.getId(), List.of());

            if (!Set.copyOf(atuais).equals(Set.copyOf(esperados))) {
                produtoRepository.salvarAtributos(produto.getId(), esperados);
                contagem.atributos++;
            }
        }
    }

    /**
     * Faz o que esta gravado voltar a bater com o que a massa declara: nome, descricao e imagem.
     * <p>
     * A carga e incremental e nunca reescreve o SKU de um produto que ja existe (D-47), entao
     * sem este passo qualquer correcao na massa ficaria so no codigo - inclusive no banco
     * publicado, que e o que a banca ve.
     * <p>
     * <b>A massa e a fonte, e este passo sobrescreve.</b> Antes ele so preenchia campo vazio,
     * regra que servia enquanto o unico caso era completar produtos criados antes de descricao
     * e imagem existirem. Quando os nomes reais entraram junto com as fotos, ela passou a ser
     * um problema: o atributo MARCA sincroniza sempre e o nome nao sincronizava nunca, e o
     * banco publicado ficaria com marca nova e nome velho. Ver D-69.
     */
    private void sincronizarApresentacoes(Contagem contagem) {
        for (Produto produto : produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA).conteudo()) {
            Apresentacao declarada = apresentacoes.get(produto.getSku());
            if (declarada == null) {
                continue;
            }

            boolean mudou = !Objects.equals(declarada.nome(), produto.getNome())
                    || !Objects.equals(declarada.descricao(), produto.getDescricao())
                    || !Objects.equals(declarada.imagemUrl(), produto.getImagemUrl());

            if (mudou) {
                produtoRepository.salvar(produto.comApresentacao(
                        declarada.nome(), declarada.descricao(), declarada.imagemUrl()));
                contagem.apresentacoes++;
            }
        }
    }
}
