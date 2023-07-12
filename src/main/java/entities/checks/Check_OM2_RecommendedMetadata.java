/*
 * Copyright 2021-22 Ontology Engineering Group, Universidad Politecnica de Madrid, Spain
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Author: Daniel Garijo and Maria Poveda
 */

package entities.checks;

import entities.Check;
import entities.Ontology;
import fair.Constants;

public class Check_OM2_RecommendedMetadata extends Check {
    public Check_OM2_RecommendedMetadata(Ontology o) {
        super(o);
        this.category_id = Constants.REUSABLE;
        this.id = Constants.OM2;
        this.title = Constants.OM2_TITLE;
        this.description = Constants.OM2_DESC;
        this.principle_id = "R1";
        this.total_tests_run = Constants.RECOMMENDED_METADATA.length;
    }
    /**
     * This check verifies whether the detected metadata is the recommended one
     */

    @Override
    public void check() {
        super.check();
        StringBuilder exp = new StringBuilder();
        for (String m: Constants.RECOMMENDED_METADATA){
            if(!this.ontology.getSupportedMetadata().contains(m)){
                exp.append(m).append(", ");
            }else{
                total_passed_tests += 1;
            }
        }
        //remove last comma
        if("".equals(exp.toString())){
            explanation = "All recommended metadata found!";
            this.status = Constants.OK;
        }else {
            this.status = Constants.ERROR;
            explanation = Constants.OM2_EXPLANATION + exp.substring(0, exp.length() - 2);
        }

        StringBuilder optional = new StringBuilder();
        for (String m : Constants.RECOMMENDED_METADATA_OPTIONAL) {
            if (!this.ontology.getSupportedMetadata().contains(m)) {
                optional.append(m).append(", ");
            }
        }
        if (!"".equals(optional.toString())){
            explanation += ". Warning: The following OPTIONAL recommended metadata could not be found: "+
                    optional.substring(0,optional.length() -2) + ". Please consider adding them if appropriate.";
        }

    }
}
