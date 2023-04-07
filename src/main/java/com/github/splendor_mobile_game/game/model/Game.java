package com.github.splendor_mobile_game.game.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.github.splendor_mobile_game.database.Database;
import com.github.splendor_mobile_game.game.enums.CardTier;
import com.github.splendor_mobile_game.game.enums.TokenType;
import com.github.splendor_mobile_game.websocket.utils.Log;

public class Game {

    private TokenList emeraldTokens;
    private TokenList sapphireTokens;
    private TokenList rubyTokens;
    private TokenList diamondTokens;
    private TokenList onyxTokens;
    private TokenList goldTokens;

    private Map<CardTier,Deck> revealedCards = new HashMap<CardTier,Deck>();
    private Map<CardTier,Deck> decks = new HashMap<CardTier,Deck>();

    private ArrayList<Noble> nobles;

    private int maxTokenStack = 7; // Default number of each token type

    private Database database;

    public Game(Database database) {
        this.database = database;

        database.loadCards();
        database.loadNobles();
    }


    public TokenList getEmeraldTokens() {
        return emeraldTokens;
    }

    public TokenList getSapphireTokens() {
        return sapphireTokens;
    }

    public TokenList getRubyTokens() {
        return rubyTokens;
    }

    public TokenList getDiamondTokens() {
        return diamondTokens;
    }

    public TokenList getOnyxTokens() {
        return onyxTokens;
    }

    public TokenList getGoldTokens() {
        return goldTokens;
    }

    public int getMaxTokenStack() {
        return maxTokenStack;
    }



    public boolean startGame(Room room) {
        if (room.getPlayerCount() < 2) return false; // Minimum number of players to start a game is 2.
        if (room.getPlayerCount() > 4) return false; // Maximum number of players to start a game is 4.

        // Calculate number of tokens of each type
        if (room.getPlayerCount() == 2) this.maxTokenStack = 4;
        if (room.getPlayerCount() == 3) this.maxTokenStack = 5;

        // Assign all tokenLists
        this.emeraldTokens  = createTokenList(TokenType.EMERALD);
        this.sapphireTokens = createTokenList(TokenType.SAPPHIRE);
        this.rubyTokens     = createTokenList(TokenType.RUBY);
        this.diamondTokens  = createTokenList(TokenType.DIAMOND);
        this.onyxTokens     = createTokenList(TokenType.ONYX);
        this.goldTokens     = createTokenList(TokenType.GOLD_JOKER);

        //Get ALL cards from database
        decks.put(CardTier.LEVEL_1,new Deck(CardTier.LEVEL_1,database.getSpecifiedCards(CardTier.LEVEL_1)));
        decks.put(CardTier.LEVEL_2,new Deck(CardTier.LEVEL_2,database.getSpecifiedCards(CardTier.LEVEL_2)));
        decks.put(CardTier.LEVEL_3,new Deck(CardTier.LEVEL_3,database.getSpecifiedCards(CardTier.LEVEL_3)));
        
        // Choose random cards from deck
        revealedCards.put(CardTier.LEVEL_1,new Deck(CardTier.LEVEL_1,getRandomCards((CardTier.LEVEL_1),4)));
        revealedCards.put(CardTier.LEVEL_2,new Deck(CardTier.LEVEL_2,getRandomCards((CardTier.LEVEL_2),4)));
        revealedCards.put(CardTier.LEVEL_3,new Deck(CardTier.LEVEL_3,getRandomCards((CardTier.LEVEL_3),4)));

        // Choose random noble cards from database
        nobles = getRandomNobles(4);//Always we draw four noblemen

        //Only for testing TO BE DELTED
        testForDuplicates(CardTier.LEVEL_1);
        testForDuplicates(CardTier.LEVEL_2);
        testForDuplicates(CardTier.LEVEL_3);
        testForDuplicatesNoble();

        return true;
    }

    //Only for testing private function TO BE DELETED
    private void testForDuplicatesNoble(){
        ArrayList<Noble> array = nobles;

        for(int i=0;i<array.size();++i){
            for(int j=0;j<array.size();++j){
                if(i!=j){
                    if(array.get(i)==array.get(j))
                        Log.ERROR("Found duplicate at i="+i+" and j="+j);
                }
            }
        }
    }

    //Only for testing private function TO BE DELETED
    private void testForDuplicates(CardTier tier){
        Deck deck1 = revealedCards.get(tier);

        for(int i=0;i<deck1.size();++i){
            for(int j=0;j<deck1.size();++j){
                if(i!=j){
                    if(deck1.get(i)==deck1.get(j))
                        Log.ERROR("Found duplicate at i="+i+" and j="+j);
                }
            }
        }
    }

//    public void takeNobleTest() //will be deleted (only test function)
//    {
//        try {
//            User u = database.getAllUsers().get(0);
//
//            ArrayList<Card> lista = database.getAllCards();
//            u.tokens.put(TokenType.EMERALD,100);
//            u.tokens.put(TokenType.ONYX,100);
//            u.tokens.put(TokenType.DIAMOND,100);
//            u.tokens.put(TokenType.SAPPHIRE,100);
//            u.tokens.put(TokenType.RUBY,100);
//
//            for (int i = 0; i < 20; i++) {
//                u.buyCard(lista.get(i));
//            }
//        }
//        catch (Exception e)
//        {
//            Log.ERROR(e.getMessage());
//
//        }
//    }





    /**
     *
     * @param tokenType -> Color of the token
     * @return TokenList -> Object representing all available tokens
     */
    private TokenList createTokenList(TokenType tokenType) {
        TokenList tokenList = new TokenList(tokenType);

        int numberOfRepeats = maxTokenStack;
        if (tokenType == TokenType.GOLD_JOKER) numberOfRepeats = 5; // There are only 5 golden tokens

        for (int i=0; i < numberOfRepeats; i++) {
            tokenList.addToken(new Token(tokenType));
        }

        return tokenList;
    }

    private Card getRandomCard(CardTier tier){
        return getRandomCards(tier,1).get(0);
    }


    /**
     *
     * @param tier -> Tier of deck from which we draw cards
     * @param amount -> Amount of elements we want to draw
     * @return Deck -> Collection of randomly picked cards
     */
    private Deck getRandomCards(CardTier tier, int amount) {
        Deck deck = decks.get(tier);

        // We draw cards until deck will be empty
        if ( deck.size() < amount) amount=deck.size();

        Deck array = new Deck(tier);

        Random rand = new Random();
        for(;amount > 0;amount--) {
            int index = rand.nextInt(deck.size()); // Get random index
            Card drawnCard =deck.remove(index);
            array.add(drawnCard);
            
            //globalIndex in that context is an index of InMemoryDatabase.allCards
            int globalIndex=database.getAllCards().indexOf(drawnCard);          
            Log.DEBUG("Card has been drawn of tier "+tier.toString()+" and index "+globalIndex);
        }

        return array;
    }

    private ArrayList<Noble> getRandomNobles(int amount){
        ArrayList<Noble> nobles = this.database.getAllNobles();
        ArrayList<Noble> array = new ArrayList<>();

        // We draw cards until deck will be empty
        if ( nobles.size() < amount) amount=nobles.size();

        Random rand = new Random();
        while(amount > 0) {
            int index = rand.nextInt(nobles.size()); // Get random index       
            array.add(nobles.remove(index));
            
            Log.DEBUG("Noble tile has been drawned");
            amount--;
        }

        return array;
    }

}
