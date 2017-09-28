# MadAgent

*MadAgent is a negotiation agent that follows “Stacked Alternating Offers Protocol for Multi-Lateral Negotiation (SAOPMN)”.*

## Developers

* **Batuhan Erden** - [@erdenbatuhan](https://github.com/erdenbatuhan)
* **Ekrem Cetinkaya** - [@ekremcet](https://github.com/ekremcet)

## About

**Multi-Lateral Negotiation:** A form of interaction in which there are more than two agents, with conflicting interests and a desire to cooperate, try to reach a mutually acceptable agreement.

MadAgent is being designed and implemented in GENIUS for multiparty negotiation - which is a multiagent system developed at the University of Delft, The Netherlands.

Typically, negotiation is viewed as a process divided into several phases. Initially, in a prenegotiation phase the negotiation domain and the issue structure related to the domain of negotiation are fixed. Negotiation may be about many things, ranging from quite personal issues such as deciding on a holiday destination to strictly business deals such as trading orange juice in international trade. These are some examples:

  * partydomain/party1 + partydomain/party2 + partydomain/party3
  * university/university7 + university/university8 + university/university9
  * energygriddomain/consumer + energygriddomain/provider + energygriddomain/producer
  
These preference profiles are represented by means of additive utility functions. For example, if there are four issues to negotiate about, the utility function can be computed by a weighted sum of the values associated with each of these issues. So, let bid = ⟨i1, i2, i3, i4⟩ be a particular bid. Then the utility u(bid) = u(i1, i2, i3, i4) (given weights w1, w2, w3, w4) can be calculated by: u(i1, i2, i3, i4) = w1 · u(i1) + w2 · u(i2) + w3 · u(i3) + w4 · u(i4 ).

It is not allowed to have access to other agents’ preferences. The negotiation agent will follow “Stacked Alternating Offers Protocol for Multi-Lateral Negotiation (SAOPMN)”. According to this protocol, the first agent will starts the negotiation with an offer that is observed by all others immediately. When an offer is made, the next party in the line can take the following actions:

  * Make a counter offer (thus rejecting and overriding the previous offer)
  * Accept the offer
  * Walk away (e.g. ending the negotiation without any agreement)
  
This process is repeated in a turn taking fashion until reaching an agreement or reaching the deadline. To reach an agreement, all parties should accept the offer. If at the deadline no agreement has been reached, the negotiation fails. It is worth noticing that you will set the deadline before the negotiation starts.

The following block provides some initial guidelines based on human experience with negotiation:

  * Orient yourself towards a win-win approach.
  * Plan and have a concrete strategy.
  * Know your reservation value, i.e. determine which bids you will never accept.
  * Create options for mutual gain.
  * Take the preferences of your opponents into account.
  * Generate a variety of possibilities before deciding what to do.
  * Pay a lot of attention to the flow of negotiation.
  * Examine the previous negotiation sessions to adapt to domain over time.
 
 

