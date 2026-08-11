type PartyType = "person" | "organization";

type PartyIdentityFieldsProps = {
  type: PartyType;
  name: string;
  phoneNumber: string;
  cpf: string;
  cnpj: string;
  birthDate: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  inputStyle: string;
  labelStyle: string;
};

export function PartyIdentityFields({
  type,
  name,
  phoneNumber,
  cpf,
  cnpj,
  birthDate,
  onChange,
  inputStyle,
  labelStyle,
}: PartyIdentityFieldsProps) {
  return (
    <>
      <div className="flex flex-col">
        <label className={labelStyle}>Nome </label>
        <input
          name="name"
          placeholder="Digite o nome "
          value={name}
          onChange={onChange}
          className={inputStyle}
          required
        />
      </div>

      <div className="flex flex-col">
        <label className={labelStyle}>Telefone</label>
        <input
          name="phoneNumber"
          type="tel"
          placeholder="(11) 99999-9999"
          value={phoneNumber}
          onChange={onChange}
          className={inputStyle}
          required
        />
      </div>

      {type === "person" ? (
        <>
          <div className="flex flex-col">
            <label className={labelStyle}>CPF</label>
            <input
              name="cpf"
              placeholder="000.000.000-00"
              value={cpf}
              onChange={onChange}
              className={inputStyle}
              required
            />
          </div>
          <div className="flex flex-col">
            <label className={labelStyle}>Data de Nascimento</label>
            <input
              name="birthDate"
              type="date"
              value={birthDate}
              onChange={onChange}
              className={inputStyle}
              required
            />
          </div>
        </>
      ) : (
        <div className="flex flex-col">
          <label className={labelStyle}>CNPJ</label>
          <input
            name="cnpj"
            placeholder="00.000.000/0000-00"
            value={cnpj}
            onChange={onChange}
            className={inputStyle}
            required
          />
        </div>
      )}
    </>
  );
}

export type { PartyType };
