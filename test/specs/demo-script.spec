# Product Demonstration
* Initialise network with the contract "../contract.csl" on the peers
      | name    | url                    |
      | Seller | https://seller.partner.deon.digital  |
      | Buyer   | https://buyer.partner.deon.digital |
      | Factoring    | https://factoring.partner.deon.digital  |

## Happy Flow
* instantiate the template named "contract" as "offer" with entrypoint "DisputeResolutionContract" and the following args on the following peers
        | args       | peers      |
        |------------|------------|
        | Seller     | Seller     |
        | Buyer      | Buyer      |

* "Seller" makes offer to "Buyer"
* pause
* "Buyer" accepts offer