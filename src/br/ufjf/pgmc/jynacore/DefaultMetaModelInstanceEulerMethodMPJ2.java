/*******************************************************************************
 * Copyright (c) 2009 Igor Knop.
 *     This file is part of JynaCore.
 * 
 *     JynaCore is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     JynaCore is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with JynaCore.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
package br.ufjf.pgmc.jynacore;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import br.ufjf.mmc.jynacore.expression.Expression;
import br.ufjf.mmc.jynacore.expression.NameOperator;
import br.ufjf.mmc.jynacore.expression.impl.DefaultNumberConstantExpression;
import br.ufjf.mmc.jynacore.expression.impl.DefaultReferenceExpression;
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceException;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceMultiRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceAuxiliary;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceProperty;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceRate;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceSingleRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceStock;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod;
import br.ufjf.mmc.jynacore.systemdynamics.Variable;
import br.ufjf.mmc.jynacore.systemdynamics.impl.DefaultVariable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Knop
 * 
 */
public class DefaultMetaModelInstanceEulerMethodMPJ2 implements
        MetaModelInstanceSimulationMethod {

   private Double initialTime;
   private Map<String, ClassInstanceRate> rates;
   private Map<String, ClassInstanceStock> levels;
   private HashMap<String, ClassInstanceAuxiliary> auxiliaries;
   private HashMap<String, ClassInstanceProperty> properties;
   private double currentTime;
   private int currentStep;
   private Double stepSize;
   private MetaModelInstance modelInstance;
   private Map<String, ClassInstanceSingleRelation> singleRelations;
   private Map<String, ClassInstanceMultiRelation> multiRelations;
   private Variable _TIME_;
   private Variable _TIME_STEP_;
   private Integer rows;
   private Integer offset;
   private static final Logger logger = Logger.getLogger("DefaultMetamodelInstanceEulerMethodMPJ2");
   private Integer cols;

   /**
    * 
    */
   public DefaultMetaModelInstanceEulerMethodMPJ2() {
      rates = new HashMap<String, ClassInstanceRate>();
      levels = new HashMap<String, ClassInstanceStock>();
      auxiliaries = new HashMap<String, ClassInstanceAuxiliary>();
      properties = new HashMap<String, ClassInstanceProperty>();
      singleRelations = new HashMap<String, ClassInstanceSingleRelation>();
      multiRelations = new HashMap<String, ClassInstanceMultiRelation>();
      currentTime = 0.0;
      currentStep = 0;
      initialTime = 0.0;
      _TIME_ = new DefaultVariable();
      _TIME_.setName("_TIME_");
      _TIME_.setExpression(new DefaultNumberConstantExpression(currentTime));
      _TIME_STEP_ = new DefaultVariable();
      _TIME_STEP_.setName("_TIME_STEP_");
      _TIME_STEP_.setExpression(new DefaultNumberConstantExpression(stepSize));
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#getInitialTime()
    */
   @Override
   public Double getInitialTime() {
      return initialTime;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#getModel()
    */
   @Override
   public MetaModelInstance getModelInstance() {
      return modelInstance;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#getStepSize()
    */
   @Override
   public Double getStepSize() {
      return stepSize;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#getTime()
    */
   @Override
   public Double getTime() {
      return initialTime + currentTime;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#reset()
    */
   @Override
   public void reset() throws Exception {
      for (Entry<String, ClassInstanceStock> entry : levels.entrySet()) {
         Object ret = entry.getValue().getInitialValue().evaluate();
         if (ret instanceof Double) {
            entry.getValue().setValue((Double) ret);
         } else if (ret instanceof ClassInstanceProperty) {
            entry.getValue().setValue(
                    ((ClassInstanceProperty) ret).getValue());
         } else if (ret instanceof ClassInstanceAuxiliary) {
            entry.getValue().setValue(
                    ((ClassInstanceAuxiliary) ret).getValue());
         }
      }
      for (Entry<String, ClassInstanceRate> entry : rates.entrySet()) {
         entry.getValue().setValue((Double) entry.getValue().getExpression().evaluate());
//			entry.getValue().setValue(null);
      }
      for (Entry<String, ClassInstanceAuxiliary> entry : auxiliaries.entrySet()) {
         entry.getValue().setValue((Double) entry.getValue().getExpression().evaluate());
      }
      currentStep = 0;
      currentTime = initialTime;
      _TIME_.getExpression().setValue(currentTime);
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#setInitialTime(java.lang.Double)
    */
   @Override
   public void setInitialTime(Double newInitialTime) {
      initialTime = newInitialTime;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#setMetaModelInstance(br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance)
    */
   @Override
   public void setMetaModelInstance(MetaModelInstance modelInstance)
           throws Exception {
      this.modelInstance = modelInstance;
      rates.clear();
      levels.clear();
      auxiliaries.clear();
      properties.clear();
      singleRelations.clear();
      multiRelations.clear();
      for (Entry<String, ClassInstance> ciEntry : modelInstance.getClassInstances().entrySet()) {
         String classInstanceName = ciEntry.getKey();
         ClassInstance classInstance = ciEntry.getValue();
         for (Entry<String, ClassInstanceItem> ciItem : classInstance.entrySet()) {
            if (ciItem.getValue() instanceof ClassInstanceStock) {
               ClassInstanceStock ciLevel = (ClassInstanceStock) ciItem.getValue();
               String ciLevelName = ciItem.getKey();
               levels.put(getKey(classInstanceName, ciLevelName), ciLevel);
            } else if (ciItem.getValue() instanceof ClassInstanceRate) {
               ClassInstanceRate ciRate = (ClassInstanceRate) ciItem.getValue();
               String ciRateName = ciItem.getKey();
               rates.put(getKey(classInstanceName, ciRateName), ciRate);
            } else if (ciItem.getValue() instanceof ClassInstanceAuxiliary) {
               ClassInstanceAuxiliary ciProc = (ClassInstanceAuxiliary) ciItem.getValue();
               String ciProcName = ciItem.getKey();
               auxiliaries.put(getKey(classInstanceName, ciProcName),
                       ciProc);
            } else if (ciItem.getValue() instanceof ClassInstanceProperty) {
               ClassInstanceProperty ciProperty = (ClassInstanceProperty) ciItem.getValue();
               String ciPropertyName = ciItem.getKey();
               properties.put(getKey(classInstanceName, ciPropertyName),
                       ciProperty);
            } else if (ciItem.getValue() instanceof ClassInstanceSingleRelation) {
               ClassInstanceSingleRelation ciSingleRelation = (ClassInstanceSingleRelation) ciItem.getValue();
               String ciSingleRelationName = ciItem.getKey();
               singleRelations.put(getKey(classInstanceName,
                       ciSingleRelationName), ciSingleRelation);
            } else if (ciItem.getValue() instanceof ClassInstanceMultiRelation) {
               ClassInstanceMultiRelation ciMultiRelation = (ClassInstanceMultiRelation) ciItem.getValue();
               String ciMultiRelationName = ciItem.getKey();
               multiRelations.put(getKey(classInstanceName,
                       ciMultiRelationName), ciMultiRelation);
            } else {
               throw new MetaModelInstanceException(
                       "Unknow Class Instance Element:"
                       + ciItem.getValue().getClass().toString());
            }
         }
      }
      setupReferences();
   }

   private void setupReferences() throws Exception {
      modelInstance.updateReferences();

      // for (Entry<String, ClassInstanceStock> lvlEntry : levels.entrySet())
      // {
      // name2ref(lvlEntry.getValue().getInitialValue());
      // }
      // for (Entry<String, ClassInstanceRate> ratEntry : rates.entrySet()) {
      // name2ref(ratEntry.getValue().getExpression());
      // }
      // for (Entry<String, ClassInstanceAuxiliary> auxEntry :
      // auxiliaries.entrySet()) {
      // name2ref(auxEntry.getValue().getExpression());
      // }
   }

   @SuppressWarnings("unused")
   private void name2ref(Expression expr) {
      if (expr == null) {
         return;
      } else if (expr.getOperator() instanceof NameOperator) {
         if (expr.getValue().equals("_TIME_")) {
            expr.setMiddleOperand(new DefaultReferenceExpression(_TIME_));
         } else if (expr.getValue().equals("_TIME_STEP_")) {
            expr.setMiddleOperand(new DefaultReferenceExpression(_TIME_STEP_));
         } else {
            expr.setMiddleOperand(new DefaultReferenceExpression(
                    getReference((String) expr.getValue())));
         }
         return;
      } else {
         name2ref(expr.getLeftOperand());
         name2ref(expr.getMiddleOperand());
         name2ref(expr.getRightOperand());
      }

   }

   private String getKey(String classInstanceName, String ciLevelName) {
      return classInstanceName + "." + ciLevelName;
   }

   private Object getReference(String name) {
      // FIXME
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#setStepSize(java.lang.Double)
    */
   @Override
   public void setStepSize(Double newStepSize) {
      stepSize = newStepSize;
      _TIME_STEP_.getExpression().setValue(stepSize);
   }

   /*
    * (non-Javadoc)
    * 
    * @see br.ufjf.mmc.jynacore.metamodel.simulator.MetaModelInstanceSimulationMethod#step()
    */
   @Override
   public void step() throws Exception {
      for (Entry<String, ClassInstanceAuxiliary> entry : auxiliaries.entrySet()) {
         ClassInstanceAuxiliary proc = entry.getValue();
         proc.setValue(null);
      }
      for (Entry<String, ClassInstanceRate> entry : rates.entrySet()) {
         ClassInstanceRate rate = entry.getValue();
         rate.setValue(null);
      }
      // Calculates all auxiliary values
      for (Entry<String, ClassInstanceAuxiliary> entry : auxiliaries.entrySet()) {
         ClassInstanceAuxiliary proc = entry.getValue();
//			if(proc.getValue()==null){
         proc.setValue((Double) proc.getExpression().evaluate());
//			}
      }
      // Calculates all rate values
      for (Entry<String, ClassInstanceRate> entry : rates.entrySet()) {
         ClassInstanceRate rate = entry.getValue();
         if (!isInRange(rate)) {
            return;
         }
         if (rate.getValue() == null && isInRange(rate)) {
            rate.setValue((Double) rate.getExpression().evaluate());
         }
      }

      // Calculates all rates effects
      for (Entry<String, ClassInstanceRate> entry : rates.entrySet()) {
         ClassInstanceRate rate = entry.getValue();
         if (!isInRange(rate)) {
            return;
         }
         if (rate.getSource() instanceof ClassInstanceStock) {
            ClassInstanceStock flevel = rate.getSource();
            flevel.setValue(flevel.getValue() - rate.getValue()
                    * getStepSize());
         }
         if (rate.getTarget() instanceof ClassInstanceStock) {
            ClassInstanceStock flevel = rate.getTarget();
            flevel.setValue(flevel.getValue() + rate.getValue()
                    * getStepSize());
         }

      }
      currentTime += stepSize;
      _TIME_.getExpression().setValue(currentTime);
      currentStep++;
   }

   private boolean isInRange(ClassInstanceItem item) {
      ClassInstance ci = item.getClassInstance();
      String ciName = ci.getName();
      String[] ciParts = ciName.split(",");
      Integer ciRow = Integer.valueOf(ciParts[0].replace("cell[", ""));
      Integer ciCol = Integer.valueOf(ciParts[1].replace("]", ""));
      Boolean isIt = (getOffset() <= ciRow && ciRow <= (getOffset() + getRows()));
      logger.log(Level.INFO, "MPJ2: {6}>cell[{0},{1}] {2} in offset={3} rows={4} cols={5}!", new Object[]{ciRow, ciCol, isIt ? "IS" : "IS NOT", getOffset(), getRows(), getCols(), ciName});


      return isIt;
   }

   public Integer getOffset() {
      return offset;
   }

   public Integer getRows() {
      return rows;
   }

   public void setOffset(Integer offset) {
      this.offset = offset;
   }

   public void setRows(Integer rows) {
      this.rows = rows;
   }

   private Integer getCols() {
      return this.cols;
   }
   
   public void setCols(Integer cols){
      this.cols = cols;
   }
}
