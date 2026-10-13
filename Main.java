import java.util.ArrayList;
import java.util.List;

/**
 * Desafio: Design Patterns com Java (Java Puro)
 * Tema: ByteBank - processamento de transacoes
 *
 * Padroes aplicados:
 *  - Singleton: AuditoriaLog (instancia unica de log)
 *  - Strategy: TaxaStrategy (calculo de taxa por tipo de conta)
 *  - Chain of Responsibility: validadores de transacao encadeados
 *  - Facade: BancoFacade (interface simples para o subsistema)
 */
public class Main {

    // ===================== SINGLETON =====================
    static class AuditoriaLog {
        private static AuditoriaLog instancia;
        private final List<String> registros = new ArrayList<>();

        private AuditoriaLog() {}

        public static synchronized AuditoriaLog getInstancia() {
            if (instancia == null) {
                instancia = new AuditoriaLog();
            }
            return instancia;
        }

        public void registrar(String msg) {
            registros.add(msg);
            System.out.println("[LOG] " + msg);
        }

        public int total() {
            return registros.size();
        }
    }

    // ===================== STRATEGY =====================
    interface TaxaStrategy {
        double calcular(double valor);
    }

    static class TaxaContaPadrao implements TaxaStrategy {
        public double calcular(double valor) { return valor * 0.02; }
    }

    static class TaxaContaPremium implements TaxaStrategy {
        public double calcular(double valor) { return valor * 0.005; }
    }

    static class TaxaContaIsenta implements TaxaStrategy {
        public double calcular(double valor) { return 0; }
    }

    // ===================== MODELO =====================
    static class Conta {
        private final String titular;
        private double saldo;
        private final TaxaStrategy taxa;

        Conta(String titular, double saldo, TaxaStrategy taxa) {
            this.titular = titular;
            this.saldo = saldo;
            this.taxa = taxa;
        }

        String getTitular() { return titular; }
        double getSaldo() { return saldo; }
        TaxaStrategy getTaxa() { return taxa; }
        void debitar(double v) { saldo -= v; }
        void creditar(double v) { saldo += v; }
    }

    static class Transacao {
        final Conta origem;
        final Conta destino;
        final double valor;

        Transacao(Conta origem, Conta destino, double valor) {
            this.origem = origem;
            this.destino = destino;
            this.valor = valor;
        }
    }

    // ============= CHAIN OF RESPONSIBILITY =============
    static abstract class Validador {
        private Validador proximo;

        Validador encadear(Validador proximo) {
            this.proximo = proximo;
            return proximo;
        }

        void validar(Transacao t) {
            verificar(t);
            if (proximo != null) {
                proximo.validar(t);
            }
        }

        protected abstract void verificar(Transacao t);
    }

    static class ValidadorValor extends Validador {
        protected void verificar(Transacao t) {
            if (t.valor <= 0) {
                throw new IllegalArgumentException("valor invalido");
            }
        }
    }

    static class ValidadorContas extends Validador {
        protected void verificar(Transacao t) {
            if (t.origem == t.destino) {
                throw new IllegalArgumentException("origem e destino iguais");
            }
        }
    }

    static class ValidadorSaldo extends Validador {
        protected void verificar(Transacao t) {
            double total = t.valor + t.origem.getTaxa().calcular(t.valor);
            if (total > t.origem.getSaldo()) {
                throw new IllegalStateException("saldo insuficiente");
            }
        }
    }

    // ===================== FACADE =====================
    static class BancoFacade {
        private final Validador cadeia;
        private final AuditoriaLog log = AuditoriaLog.getInstancia();

        BancoFacade() {
            ValidadorValor primeiro = new ValidadorValor();
            primeiro.encadear(new ValidadorContas()).encadear(new ValidadorSaldo());
            this.cadeia = primeiro;
        }

        void transferir(Conta origem, Conta destino, double valor) {
            Transacao t = new Transacao(origem, destino, valor);
            try {
                cadeia.validar(t);
                double taxa = origem.getTaxa().calcular(valor);
                origem.debitar(valor + taxa);
                destino.creditar(valor);
                log.registrar(String.format("OK: %s -> %s | valor=%.2f taxa=%.2f",
                        origem.getTitular(), destino.getTitular(), valor, taxa));
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.registrar(String.format("FALHA: %s -> %s | valor=%.2f | motivo: %s",
                        origem.getTitular(), destino.getTitular(), valor, e.getMessage()));
            }
        }
    }

    // ===================== EXECUCAO =====================
    public static void main(String[] args) {
        Conta ana = new Conta("Ana", 1000, new TaxaContaPadrao());
        Conta bruno = new Conta("Bruno", 500, new TaxaContaPremium());
        Conta carla = new Conta("Carla", 200, new TaxaContaIsenta());

        BancoFacade banco = new BancoFacade();

        banco.transferir(ana, bruno, 300);    // ok, taxa 2%
        banco.transferir(bruno, carla, 100);  // ok, taxa 0,5%
        banco.transferir(carla, ana, 1000);   // saldo insuficiente
        banco.transferir(ana, ana, 50);       // contas iguais
        banco.transferir(ana, carla, -10);    // valor invalido

        System.out.println();
        System.out.printf("Saldo Ana:   %.2f%n", ana.getSaldo());
        System.out.printf("Saldo Bruno: %.2f%n", bruno.getSaldo());
        System.out.printf("Saldo Carla: %.2f%n", carla.getSaldo());
        System.out.println("Registros no log: " + AuditoriaLog.getInstancia().total());
    }
}
