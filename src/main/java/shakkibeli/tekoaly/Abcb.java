package shakkibeli.tekoaly;

import abcb.InputReader.Openings;
import abcb.algorithm.AlphaBeta;
import abcb.datastructure.MyRecord;
import abcb.simulate.Generator;
import abcb.simulate.Position;
import abcb.util.MoveConverter;
import abcb.util.Randomizer;
import java.io.IOException;
import java.util.List;
import shakkibeli.logiikka.Pelilogiikka;
import shakkibeli.logiikka.Siirto;
import shakkibeli.nappula.Nappula;
import shakkibeli.nappula.Vari;
import static shakkibeli.nappula.Vari.MUSTA;
import static shakkibeli.nappula.Vari.VALKOINEN;

/**
 * Yhistää uuden botin ja vanhan käyttöliittymän. Finenglish koska käyttis
 * suomeks ja botti enkuks.
 *
 * @author Eero
 */
public class Abcb {

    private Pelilogiikka pelo;
    private List<Nappula> nappulat;
    private Vari vari;
    private AlphaBeta ab;
    private int depth;
    private MoveConverter moveConverter;
    private MyRecord history;
    private Position previous;
    private Openings openings;
    private Generator generator;
    private int moveNumber = 1;
    private boolean looping;
    private Randomizer randomizer;

    public Abcb(Pelilogiikka pelo, List<Nappula> nappulat, Vari vari) throws IOException {
        this.pelo = pelo;
        this.nappulat = nappulat;
        this.vari = vari;
        this.ab = new AlphaBeta();
        this.depth = 5;
        this.history = new MyRecord<String>();
        this.openings = new Openings();
        this.generator = new Generator();
        this.moveConverter = new MoveConverter();
        this.randomizer = new Randomizer();
    }

    public void suoritaSiirto(Vari vari) {
        System.out.println("\n\nMOVE NUMBER " + moveNumber++);
        System.out.println("ihan eka");
        Position currentPosition = new Position();
        for (Nappula nappi : nappulat) {
            if (nappi.onkoSyoty()) {
                continue;
            }
            currentPosition.board[nappi.getY()][nappi.getX()] = nappi.getIntegerValue();
        }

        // Gui doesnt add into history, so we have to do it here..
        if (previous != null) {
            addToHistory(previous, currentPosition);
        } else if (this.vari == MUSTA) {
            addToHistory(generator.createStartingPosition(), currentPosition);
        }

        /**
         * (OPENING / ALPHA-BETA) SEARCH *
         */
        Position nextPosition = figureOutNextPosition(currentPosition);
        /**
         * SEARCH OVER *
         */
        if (looping) {
            MyRecord<Position> nextPositions = generator.getNextPositions(currentPosition);
            nextPosition = nextPositions.get(randomizer.generateRandomInt(nextPositions.size() - 1));
            looping = false;
        }

        if (nextPosition != null) {
            addToHistory(currentPosition, nextPosition);
            Siirto siirto = new Siirto(currentPosition, nextPosition, nappulat);
            System.out.println("\n" + siirto.getNappula().toString());
            System.out.println(siirto.toString());
            pelo.liikutaNappulaa(new Siirto(currentPosition, nextPosition, nappulat));
            previous = nextPosition;
        } else {
            TekoalynAuttaja ta = new TekoalynAuttaja(pelo);
            if (!ta.suoritaSiirtoJosMahdollista(nappulat)) {
                System.out.println("Could not find next move!");
                if (pelo.shakkaako()) {
                    pelo.setJatkuu(2);
                } else {
                    pelo.setJatkuu(3);
                }
                System.out.println("Game over");
            }
        }
    }
    
    public Position figureOutNextPosition(Position currentPosition) {
        if (history.size() < 20) {
            if (openings.search(historyToString())) {
                String moveStr = openings.getNextMove();
                Siirto siirto = getNextSiirto(currentPosition, moveStr);
                if (siirto != null) {
                    Position p = new Position();
                    p.clonePosition(currentPosition, siirto.getX(), siirto.getY());
                    p.board[siirto.getUusiY()][siirto.getUusiX()] = siirto.getNappula().getIntegerValue();
                    System.out.println("opening found! " + openings.getOpeningName());
                    return p;
                }
                System.out.println("could not find opening..");
            }
        }
        System.out.println("search failed... historyToString = " + historyToString());
        currentPosition.whitesMove = vari == VALKOINEN;
        return ab.calculateNextPosition(currentPosition, depth, vari == VALKOINEN);
    }

    private Siirto getNextSiirto(Position currentPosition, String search) {
        currentPosition.whitesMove = this.vari == VALKOINEN;
        MyRecord<Position> nextPositions = generator.getNextPositions(currentPosition);
        for (int i = 0; i < nextPositions.size(); i++) {
            Position nextPosition = nextPositions.get(i);
            Siirto siirto = new Siirto(currentPosition, nextPosition, nappulat);
            System.out.println("siirto[" + i + "] = " + siirto.toChessNotation());
            if (siirto.toChessNotation().equals(search)) {
                return siirto;
            }
        }
        return null;
    }

    public void addToHistory(Position current, Position next) {
        String s = moveConverter.positionsToChessNotation(current, next);
        System.out.println("s = " + s);
        history.add(s);
    }

    public String historyToString() {
        int k = 1;
        String ret = "";
        for (int i = 0; i < history.size(); i += 2) {
            if (k > 1) {
                ret += " ";
            }
            if (i + 1 < history.size()) {
                ret += "" + k + ". " + history.get(i) + " " + history.get(i + 1);
            } else {
                ret += k + ". " + history.get(i);
            }
            k += 1;
        }
        return ret;
    }

    public Pelilogiikka getPelo() {
        return pelo;
    }

    public void setPelo(Pelilogiikka pelo) {
        this.pelo = pelo;
    }

    public List<Nappula> getNappulat() {
        return nappulat;
    }

    public void setNappulat(List<Nappula> nappulat) {
        this.nappulat = nappulat;
    }

    public Vari getVari() {
        return vari;
    }

    public void setVari(Vari vari) {
        this.vari = vari;
    }

    public void forceOutFromLoop() {
        this.looping = true;
    }
}
