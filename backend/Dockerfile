# Imagem do backend do Merlin Route Finder.
#
# Container em vez do runtime Java nativo do provedor: assim o ambiente que roda no servidor
# e o mesmo que roda na maquina de qualquer integrante, e a versao do Java e do Maven vem do
# que o repositorio ja declara, nao do que o provedor decidir instalar.

# --------------------------------------------------------------------------- build
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

# O wrapper e as dependencias vem antes do codigo de proposito: enquanto o pom.xml nao mudar,
# o Docker reaproveita esta camada e o build nao baixa a internet inteira a cada deploy.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# O bit de execucao nao sobrevive ao clone em Windows.
RUN chmod +x ./mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/

# Sem testes aqui, e nao por descuido: a suite roda antes de cada commit no fluxo do time
# (planejar -> implementar -> documentar -> revisar -> commitar), e repeti-la no build do
# provedor so tornaria cada publicacao mais lenta e mais fragil - um teste instavel deixaria
# de ser um aviso e passaria a impedir o deploy.
RUN ./mvnw -B clean package -DskipTests

# --------------------------------------------------------------------------- runtime
FROM eclipse-temurin:21-jre AS runtime

# Usuario sem privilegios: se a aplicacao for comprometida, o atacante nao herda root no
# container. Custa uma linha e e a pratica esperada de qualquer imagem publicada.
RUN useradd --system --create-home --shell /usr/sbin/nologin merlin
USER merlin

WORKDIR /app
COPY --from=build /build/target/backend-*.jar app.jar

# O provedor injeta a porta em PORT; application.yml ja a respeita.
EXPOSE 8080

# MaxRAMPercentage porque o container tem limite de memoria proprio e a JVM, sozinha,
# dimensiona o heap pela memoria da MAQUINA - num tier gratuito de 512 MB isso a faria
# reservar mais do que existe e morrer por falta de memoria.
#
# SerialGC porque os coletores paralelos criam threads e estruturas que so compensam em heaps
# grandes; num container pequeno eles cobram memoria que faz falta para a aplicacao.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
