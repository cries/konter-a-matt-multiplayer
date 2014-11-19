import java.awt.Color;

import ch.aplu.jcardgame.*;
import ch.aplu.jcardgame.Hand.CardAlignment;
import ch.aplu.jgamegrid.*;
import ch.aplu.tcp.*;
import ch.aplu.util.Monitor;

public class CardTable extends CardGame
{
  public static enum Suit
  {
    SPADES, HEARTS, DIAMONDS, CLUBS
  }

  public static enum Rank
  {
    ACE, KING, QUEEN, JACK, TEN, NINE
  }
  
  protected static Deck deck = new Deck(Suit.values(), Rank.values(), "cover");
  private final int nbPlayers = 4;
  private final int nbStartCards = 3;
  private final int handWidth = 300;
  private final Location[] handLocations = {
	new Location(350, 625),
	new Location(75, 350),
	new Location(350, 75),
	new Location(625, 350)
  };
  private final Location[] stockLocations = {
    new Location(570, 625),
    new Location(75, 570),
    new Location(130, 75),
    new Location(625, 130)
  };
  private final Location talonLocation = new Location(350, 350);
  private final Location lineLocation = new Location(275, 350);
  private Hand[] hands = new Hand[nbPlayers];
  private Hand[] bids = new Hand[nbPlayers];
  private Hand line = new Hand(deck);
  private int currentPlayerIndex;
  private String[] playerNames;
  private TcpAgent agent;
  private final Location toolBarLocation = new Location(230,500);
  private GGTextField trompText = new GGTextField(this, "", new Location(550,10), false);
  private ToolBarText textItem = 
			new ToolBarText("Tromp auswielen:", 30);
		private ToolBarSeparator separator0 = 
			new ToolBarSeparator(2, 30, Color.black);
		private ToolBarStack spades =
			new ToolBarStack("sprites/spades.gif", 1);
		private ToolBarStack hearts =
			new ToolBarStack("sprites/hearts.gif", 1);
		private ToolBarStack diamonds =
			new ToolBarStack("sprites/diamonds.gif", 1);
		private ToolBarStack clubs =
			new ToolBarStack("sprites/clubs.gif", 1);
		private ToolBarSeparator separator1 =
			new ToolBarSeparator(2, 30, Color.black);
		private ToolBarItem okBtn =
			new ToolBarItem("sprites/ok30.gif", 2);
		private ToolBar toolBar = new ToolBar(this);
		
  private int targetCount = 0;
  private int firstPlayer = 0;
  private int nbMovesInRound = 0;
  
  private Card trump = null;

  public CardTable(TcpAgent agent, String[] playerNames,int currentPlayerIndex)
  {
	super(700, 700, 30);
    this.agent = agent;
    this.playerNames = new String[nbPlayers];
    for (int i = 0; i < nbPlayers; i++)
      this.playerNames[i] = playerNames[i];
    this.currentPlayerIndex = currentPlayerIndex;
    setTitle("Current player's name: " + playerNames[currentPlayerIndex]);
  }

  protected void initFirstHands(int[] cardNumbers)
  {
    for (int i = 0; i < nbPlayers; i++)
    {
      hands[i] = new Hand(deck);
      for (int k = 0; k < nbStartCards; k++)
        hands[i].insert(cardNumbers[i * nbStartCards + k], false);
    }
    
    RowLayout myLineLayout = new RowLayout(lineLocation, 250);
    myLineLayout.setCardAlignment(CardAlignment.FIRST);
    line.setView(this, myLineLayout);
    RowLayout[] layouts = new RowLayout[nbPlayers];
    
    for (int i = 0; i < nbPlayers; i++)
    {
      int k = (currentPlayerIndex + i) % nbPlayers;
      layouts[k] = new RowLayout(handLocations[i], handWidth);
      layouts[k].setRotationAngle(90 * i);
      if (k != currentPlayerIndex)
        hands[k].setVerso(true);
      hands[k].setView(this, layouts[k]);
      hands[k].setTargetArea(new TargetArea(lineLocation));
      hands[k].draw();
    }
  }
  
  protected void initLastHands(int[] cardNumbers)
  {
	
    for (int i = 0; i < nbPlayers; i++)
    {
      for (int k = 0; k < nbStartCards; k++)
        hands[i].insert(cardNumbers[(i) * nbStartCards + k], false);
    }
    
    hands[currentPlayerIndex].sort(Hand.SortType.SUITPRIORITY, false);
    hands[currentPlayerIndex].addCardListener(new CardAdapter()
    {
      public void leftDoubleClicked(Card card)
      {
    	final Card firstCard;
    	
    	if (currentPlayerIndex != firstPlayer) {
    		firstCard = line.getFirst();
    		if (firstCard.getSuit() == trump.getSuit()
				|| (firstCard.getRank() == Rank.QUEEN  
					&& (firstCard.getSuit() == Suit.SPADES 
					|| firstCard.getSuit() == Suit.HEARTS 
					|| firstCard.getSuit() == Suit.DIAMONDS))) {
				// War dei gewielten Kaart eng Tromp?
				if (card.getSuit() == trump.getSuit()
					|| (card.getRank() == Rank.QUEEN 
						&& (card.getSuit() == Suit.SPADES
						|| card.getSuit() == Suit.HEARTS
						|| card.getSuit() == Suit.DIAMONDS))) {
					// Dei gewielten Kaart spillen
					agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
				              currentPlayerIndex, card.getCardNumber());
					makeBid(card);
					agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
				} else {
					// Huet den Spiller iwerhapt Tremp?
					if (hands[currentPlayerIndex].extractCardsWithSuit(trump.getSuit()).getNumberOfCards() > 0
						|| hands[currentPlayerIndex].extractCardsWithRank(Rank.QUEEN).getNumberOfCardsWithSuit(Suit.SPADES) > 0
						|| hands[currentPlayerIndex].extractCardsWithRank(Rank.QUEEN).getNumberOfCardsWithSuit(Suit.HEARTS) > 0
						|| hands[currentPlayerIndex].extractCardsWithRank(Rank.QUEEN).getNumberOfCardsWithSuit(Suit.DIAMONDS) > 0) {
						// Tromp muss bekannt gin
						setStatusText("Tromp bekennen!");
					} else {
						// Dei gewielten Kaart spillen
						agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
					              currentPlayerIndex, card.getCardNumber());
						makeBid(card);
						agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
					}
				}
			} else {
				// As dei gewielten Kaart eng Tromp?
				if (card.getSuit() == trump.getSuit()
					|| (card.getRank() == Rank.QUEEN 
    				  	&& (card.getSuit() == Suit.SPADES
    					|| card.getSuit() == Suit.HEARTS
    					|| card.getSuit() == Suit.DIAMONDS))) {
					// Gewielte Kaart spillen
					agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
				              currentPlayerIndex, card.getCardNumber());
					makeBid(card);
					agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
				} else {
					// Huet dei gewielten Kaart dei selwecht Suit wei dei eischt?
					// (huet bekannt)
					if (card.getSuit() == firstCard.getSuit()) {
						// Gewielte Kaart spillen
						agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
					              currentPlayerIndex, card.getCardNumber());
						makeBid(card);
						agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
					} else {
						// Kann den Spiller bekennen ? (Ausser dei eenzeg Kaart vun deem Suit as eng Tromp)
						if ((hands[currentPlayerIndex].extractCardsWithSuit(firstCard.getSuit()).getNumberOfCards() > 0) || 
							((hands[currentPlayerIndex].extractCardsWithSuit(firstCard.getSuit()).getNumberOfCards() > 0) && (hands[currentPlayerIndex].getNumberOfCardsWithRank(Rank.QUEEN) == 0))) {
							// Et muss bekannt gin
							setStatusText(firstCard.getSuit() + " bekennen!");
						} else {
							// Dei gewielten Kaart spillen
							agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
						              currentPlayerIndex, card.getCardNumber());
							makeBid(card);
							agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
						}
					}
				}
			}
    	} else {
    		hands[currentPlayerIndex].setTouchEnabled(false);
            agent.sendCommand("", CardPlayer.Command.CARD_TO_LINE,
              currentPlayerIndex, card.getCardNumber());
            makeBid(card);
            agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
    	}
      }
      
      public void makeBid(Card card) {
  		card.transfer(line, true);
  		if (nbMovesInRound == 3){
			setStatusText("Round end...");
        	  nbMovesInRound = 0;
        	  //currentPlayer = transferToWinner();
        	  firstPlayer = currentPlayerIndex;
        	  //stocks[currentPlayer].draw();
		} else
			nbMovesInRound++;
		if (hands[currentPlayerIndex].isEmpty())
			setStatusText("Game over");
		else {
			//setStatusText("Current player: " + currentPlayer);
			//hands[currentPlayer].setTouchEnabled(true);
		}
      }
    });

    RowLayout[] layouts = new RowLayout[nbPlayers];
    for (int i = 0; i < nbPlayers; i++)
    {
      int k = (currentPlayerIndex + i) % nbPlayers;
      layouts[k] = new RowLayout(handLocations[i], handWidth);
      layouts[k].setRotationAngle(90 * i);
      if (k != currentPlayerIndex)
        hands[k].setVerso(true);
      hands[k].setView(this, layouts[k]);
      hands[k].setTargetArea(new TargetArea(lineLocation));
      hands[k].draw();
    }
    agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
  }

  protected void moveCardToLine(int playerIndex, int cardNumber)
  {
    Card card = hands[playerIndex].getCard(cardNumber);
    card.setVerso(false);
    hands[playerIndex].transfer(card, line, true);
    agent.sendCommand("", CardPlayer.Command.READY_TO_PLAY);
  }

  protected void stopGame(String client)
  {
    setStatusText(client + " disconnected. Game stopped.");
    setMouseEnabled(false);
    doPause();
  }

  protected void setMyTurn()
  {
    setStatusText(
      "It's your turn. Double click on one of your cards to play it.");
    hands[currentPlayerIndex].setTouchEnabled(true);
  }

  protected void setOtherTurn()
  {
    setStatusText("Wait for you turn.");
  }
  
  protected void initToolBar() {
		toolBar.addItem(textItem, separator0, spades, hearts, diamonds, clubs, separator1, okBtn);
		toolBar.show(toolBarLocation);
		toolBar.addToolBarListener(new ToolBarAdapter() {
			public void leftPressed(ToolBarItem item) {
				if (item == okBtn) {
					okBtn.show(1);
					Card card = getSelectedCard(deck);
					if (card != null) {
//						// Tromp setzen
						toolBar.setLocation(new Location(-400, -400));
						int trumpIndex = -1;
						if (card.getSuit() == Suit.CLUBS) {
							trumpIndex = 0;
						} else {
							if (card.getSuit() == Suit.DIAMONDS) {
								trumpIndex = 1;
							} else {
								if (card.getSuit() == Suit.HEARTS) {
									trumpIndex = 2;
								} else {
									if (card.getSuit() == Suit.SPADES) {
										trumpIndex = 3;
									}
								}
							}
						}
						agent.sendCommand("", CardPlayer.Command.TRUMP_SET, trumpIndex);
					} else {
						setStatusText("Fehler");
					}
				} else {
					ToolBarStack stackItem = (ToolBarStack)item;
			        if (stackItem.isSelected()) {
			            stackItem.showNext();
			        } else {
			            deselectAll();
			            stackItem.setSelected(true);
			        }
				}
			}
			
			public void leftReleased(ToolBarItem item) {
				if (item == okBtn)
					item.show(0);
		    }
		});
	}
  
  private void deselectAll() {
	    spades.setSelected(false);
	    hearts.setSelected(false);
	    diamonds.setSelected(false);
	    clubs.setSelected(false);
  }
  
  @SuppressWarnings("unchecked")
	private Card getSelectedCard(Deck deck) {
		Card card = null;
	    int[] ids = ToolBarStack.getSelectedItemIds();
	    for (int i = 0; i < ids.length; i++) {
	    	int id = ids[i];
	        if (id != -1) {
	        	deck.getSuit(i);
	        	deck.getRank(id);
	        	card = new Card(deck, deck.getSuit(i), deck.getRank(id));
	        	break;
	        }
	    }
	    return card;
	}
  
  protected void setTrumpText(int trumpIndex) {
	  String trompTxt = "";
	  switch (trumpIndex) {
	  case 0:
		  trompTxt = "Kräizer";
		  trump = new Card(deck, Suit.CLUBS, Rank.ACE);
	  break;
	  case 1:
		  trompTxt = "Rauten";
		  trump = new Card(deck, Suit.DIAMONDS, Rank.ACE);
	  break; 
	  case 2:
		  trompTxt = "Häerzer";
		  trump = new Card(deck, Suit.HEARTS, Rank.ACE);
	  break; 
	  case 3:
		  trompTxt = "Schëppen";
		  trump = new Card(deck, Suit.SPADES, Rank.ACE);
	  break;
	  }
	  setStatusText(trompTxt + " as Tromp!");
	  trompText.setText(trompTxt + " as Tromp");
	  trompText.setBgColor(Color.white);
      trompText.show();
	  
  }
}